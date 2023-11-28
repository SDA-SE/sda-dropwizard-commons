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
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoDatabase;
import com.mongodb.event.ServerClosedEvent;
import com.mongodb.event.ServerDescriptionChangedEvent;
import com.mongodb.event.ServerListener;
import com.mongodb.event.ServerOpeningEvent;
import com.mongodb.internal.connection.ServerAddressHelper;
import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.ImmutableNet;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.transitions.ImmutableMongod;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.reverse.transitions.ImmutableStart;
import de.flapdoodle.reverse.transitions.Start;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.lang3.StringUtils;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.sdase.commons.server.mongo.testing.internal.DownloadConfigFactoryUtil;

public class StartLocalMongoDb {

  private final boolean enableScripting;
  protected final IFeatureAwareVersion version;

  private AutoCloseable mongodStopper;

  private volatile boolean started;

  private final long timeoutMs;
  protected final String username;
  protected final String password;
  protected final String database;
  protected String connectionString;

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
      this.connectionString =
          "mongodb://"
              + username
              + ":"
              + password
              + "@"
              + host.getHostName()
              + ":"
              + serverPort
              + "/"
              + database;
      if (StringUtils.isNotBlank(getOptions())) {
        this.connectionString += "?" + getOptions();
      }

      ImmutableMongod.Builder mongodBuilder =
          Mongod.builder()
              .net(createNet(host, serverPort))
              .mongodArguments(createMongodArguments())
              .downloadPackage(DownloadConfigFactoryUtil.createDownloadPackage());
      DownloadConfigFactoryUtil.createPackageOfDistribution(version)
          .ifPresent(mongodBuilder::packageOfDistribution);

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

      try (MongoClient mongoClient = new MongoClient(getHosts(), options)) {
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

  public String getHosts() {
    return new ConnectionString(this.connectionString).getHosts().get(0);
  }

  public String getDatabase() {
    return database;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getOptions() {
    return "authSource=" + getDatabase();
  }

  public String getConnectionString() {
    return connectionString;
  }

  /**
   * @return the version of the MongoDB instance which is associated with this MongoDbClassExtension
   */
  public String getServerVersion() {
    try (MongoClient client = createClient()) {
      return client
          .getDatabase(getDatabase())
          .runCommand(new BsonDocument("buildinfo", new BsonString("")))
          .get("version")
          .toString();
    }
  }

  public MongoClient createClient() {
    return new MongoClient(
        ServerAddressHelper.createServerAddress(getHosts()),
        MongoCredential.createCredential(username, database, password.toCharArray()),
        MongoClientOptions.builder().build());
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
