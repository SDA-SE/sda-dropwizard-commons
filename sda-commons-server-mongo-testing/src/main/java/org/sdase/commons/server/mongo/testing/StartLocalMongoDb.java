package org.sdase.commons.server.mongo.testing;

import static de.flapdoodle.net.Net.freeServerPort;
import static de.flapdoodle.net.Net.getLocalHost;
import static java.lang.Runtime.getRuntime;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.MongoDatabase;
import com.mongodb.event.ServerClosedEvent;
import com.mongodb.event.ServerDescriptionChangedEvent;
import com.mongodb.event.ServerListener;
import com.mongodb.event.ServerOpeningEvent;
import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.ImmutableNet;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.packageresolver.Command;
import de.flapdoodle.embed.mongo.packageresolver.PlatformPackageResolver;
import de.flapdoodle.embed.mongo.transitions.ImmutableMongod;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.process.config.store.ImmutablePackage;
import de.flapdoodle.embed.process.config.store.Package;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.distribution.Version;
import de.flapdoodle.os.CommonOS;
import de.flapdoodle.os.Platform;
import de.flapdoodle.reverse.Transition;
import de.flapdoodle.reverse.transitions.ImmutableStart;
import de.flapdoodle.reverse.transitions.Start;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import org.sdase.commons.server.dropwizard.bundles.SystemPropertyAndEnvironmentLookup;

public class StartLocalMongoDb implements MongoDb {

  private static final String EMBEDDED_MONGO_DOWNLOAD_PATH_ENV_NAME =
      "EMBEDDED_MONGO_DOWNLOAD_PATH";

  private final boolean enableScripting;
  protected final IFeatureAwareVersion version;

  private AutoCloseable mongodStopper;

  private volatile boolean started;

  private final long timeoutMs;
  protected final String username;
  protected final String password;
  protected final String database;
  protected ConnectionString mongoConnectionString;

  protected StartLocalMongoDb(
      String username,
      String password,
      String database,
      boolean enableScripting,
      IFeatureAwareVersion version,
      long timeoutMs) {

    this.version = requireNonNull(version, "version");
    this.username = requireNonNull(username, "username");
    this.password = requireNonNull(password, "password");
    this.database = requireNonNull(database, "database");
    this.enableScripting = enableScripting;
    this.timeoutMs = timeoutMs;
  }

  protected void startMongo() {
    if (started) {
      return;
    }

    try {
      InetAddress host = getLocalHost();
      int serverPort = freeServerPort(host);
      var connectionString =
          "mongodb://"
              + username
              + ":"
              + password
              + "@"
              + host.getHostName()
              + ":"
              + serverPort
              + "/"
              + database
              + "?authSource="
              + database;
      this.mongoConnectionString = new ConnectionString(connectionString);

      ImmutableMongod.Builder mongodBuilder =
          Mongod.builder()
              .net(createNet(host, serverPort))
              .mongodArguments(createMongodArguments());
      createPackageOfDistribution(version).ifPresent(mongodBuilder::packageOfDistribution);

      Mongod mongod = mongodBuilder.build();

      this.mongodStopper = mongod.start(version);

      final CountDownLatch countDownLatch = new CountDownLatch(1);

      final MongoClientOptions options =
          MongoClientOptions.builder()
              .addServerListener(
                  new ServerListener() {
                    @Override
                    public void serverOpening(final ServerOpeningEvent event) {
                      countDownLatch.countDown();
                    }

                    @Override
                    public void serverClosed(final ServerClosedEvent event) {
                      // no action required
                    }

                    @Override
                    public void serverDescriptionChanged(
                        final ServerDescriptionChangedEvent event) {
                      // no action required
                    }
                  })
              .build();

      try (MongoClient mongoClient =
          new MongoClient(getMongoConnectionString().getHosts().get(0), options)) {
        // ensure MongoDB is available before proceeding
        if (!countDownLatch.await(timeoutMs, MILLISECONDS)) {
          throw new IllegalStateException("Timeout, MongoDB not started.");
        }

        // Create the database user for the test context
        createDatabaseUser(mongoClient);
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    started = true;

    // safety net
    getRuntime().addShutdownHook(new Thread(this::stopMongo, "shutdown mongo"));
  }

  private static ImmutableStart<Net> createNet(InetAddress host, int serverPort) {
    return Start.to(Net.class)
        .initializedWith(
            ImmutableNet.builder()
                .bindIp(host.getHostAddress())
                .port(serverPort)
                .isIpv6(false)
                .build());
  }

  private ImmutableStart<MongodArguments> createMongodArguments() {
    Map<String, String> extraArgs = enableScripting ? Map.of() : Map.of("--noscripting", "");

    return Start.to(MongodArguments.class)
        .initializedWith(MongodArguments.defaults().withArgs(extraArgs));
  }

  private Optional<Transition<Package>> createPackageOfDistribution(Version version) {
    // Normally the mongod executable is downloaded directly from the
    // mongodb web page, however sometimes this behavior is undesired. Some
    // cases are proxy servers, missing internet access, or not wanting to
    // download executables from untrusted sources.
    //
    // Optional it is possible to download it from a source configured in
    // the environment variable:
    String embeddedMongoDownloadPath =
        new SystemPropertyAndEnvironmentLookup().lookup(EMBEDDED_MONGO_DOWNLOAD_PATH_ENV_NAME);
    if (embeddedMongoDownloadPath == null) {
      return Optional.empty();
    } else {
      // creates the package with the relative URL based on the OS selected, e.g.: Windows, Linux
      ImmutablePackage relativePackage =
          Package.builder()
              .from(
                  new PlatformPackageResolver(Command.MongoD)
                      .packageFor(Distribution.of(version, Platform.detect(CommonOS.list()))))
              .build();

      // creating new client with the relativePackage config, but adding the custom http host
      // information to the URL
      ImmutablePackage downloadPackage =
          ImmutablePackage.copyOf(relativePackage)
              .withUrl(embeddedMongoDownloadPath + relativePackage.url());

      return Optional.of(Start.to(Package.class).initializedWith(downloadPackage));
    }
  }

  protected void stopMongo() {
    try {
      if (started && mongodStopper != null) {
        mongodStopper.close();
        started = false;
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to stop MongoDB", e);
    }
  }

  public String getConnectionString() {
    return mongoConnectionString.getConnectionString();
  }

  public ConnectionString getMongoConnectionString() {
    return mongoConnectionString;
  }

  private void createDatabaseUser(MongoClient mongoClient) {
    MongoDatabase db = mongoClient.getDatabase(database);

    final BasicDBObject createUserCommand =
        new BasicDBObject("createUser", username)
            .append("pwd", password)
            .append(
                "roles",
                Collections.singletonList(
                    new BasicDBObject("role", "readWrite").append("db", database)));
    db.runCommand(createUserCommand);
  }
}
