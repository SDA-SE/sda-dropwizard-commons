package org.sdase.commons.server.mongo.testing;

import static java.lang.Runtime.getRuntime;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoDatabase;
import com.mongodb.event.ServerClosedEvent;
import com.mongodb.event.ServerDescriptionChangedEvent;
import com.mongodb.event.ServerListener;
import com.mongodb.event.ServerOpeningEvent;
import com.mongodb.internal.connection.ServerAddressHelper;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.Defaults;
import de.flapdoodle.embed.mongo.config.ImmutableMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.process.config.store.DownloadConfig;
import de.flapdoodle.embed.process.config.store.HttpProxyFactory;
import de.flapdoodle.embed.process.config.store.ImmutableDownloadConfig;
import de.flapdoodle.embed.process.config.store.ProxyFactory;
import de.flapdoodle.embed.process.config.store.SameDownloadPathForEveryDistribution;
import de.flapdoodle.embed.process.runtime.Network;
import de.flapdoodle.embed.process.store.ExtractedArtifactStore;
import de.flapdoodle.embed.process.store.ImmutableExtractedArtifactStore;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit Test rule for running a MongoDB instance alongside the (integration) tests. Can be
 * configured with custom user credentials and database name. Use {@link #getHost()} to retrieve the
 * host to connect to.
 *
 * <p>Example usage:
 *
 * <pre>
 * &#64;ClassRule
 * public static final MongoDbRule RULE = MongoDbRule
 *     .builder()
 *     .withDatabase("my_db")
 *     .withUsername("my_user")
 *     .withPassword("my_s3cr3t")
 *     .build();
 * </pre>
 */
public class StartLocalMongoDbRule extends ExternalResource implements MongoDbRule {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // Initialization-on-demand holder idiom
  private static class LazyHolder {
    static final MongodStarter INSTANCE = getMongoStarter();

    private static MongodStarter getMongoStarter() {
      ImmutableExtractedArtifactStore.Builder artifactStoreBuilder =
          ExtractedArtifactStore.builder()
              .from(Defaults.extractedArtifactStoreFor(Command.MongoD))
              .downloadConfig(createDownloadConfig());

      return MongodStarter.getInstance(
          Defaults.runtimeConfigFor(Command.MongoD)
              .artifactStore(artifactStoreBuilder.build())
              .build());
    }

    private static Optional<ProxyFactory> createProxyFactory() {
      String httpProxy = System.getenv("http_proxy");
      if (httpProxy != null) {
        try {
          URL url = new URL(httpProxy);

          if (url.getUserInfo() != null) {
            configureAuthentication(url);
          }

          return Optional.of(new HttpProxyFactory(url.getHost(), url.getPort()));
        } catch (MalformedURLException exception) {
          LOG.error("http_proxy could not be parsed.");
        }
      }
      return Optional.empty();
    }

    private static void configureAuthentication(URL url) {
      String userInfo = url.getUserInfo();
      int pos = userInfo.indexOf(':');
      if (pos >= 0) {
        String username = userInfo.substring(0, pos);
        String password = userInfo.substring(pos + 1);

        Authenticator.setDefault(
            new Authenticator() {
              @Override
              protected PasswordAuthentication getPasswordAuthentication() {
                // Only provide the credentials to the specified
                // origin
                if (getRequestorType() == RequestorType.PROXY
                    && getRequestingHost().equalsIgnoreCase(url.getHost())
                    && url.getPort() == getRequestingPort()) {
                  return new PasswordAuthentication(username, password.toCharArray());
                }
                return null;
              }
            });

        // Starting with Java 8u111, basic auth is not supported
        // for https by default.
        // jdk.http.auth.tunneling.disabledSchemes can be used to
        // enable it again.
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
      } else {
        LOG.error("http_proxy user info could not be parsed.");
      }
    }

    private static DownloadConfig createDownloadConfig() {
      ImmutableDownloadConfig.Builder downloadConfigBuilder =
          Defaults.downloadConfigFor(Command.MongoD).proxyFactory(createProxyFactory());

      // Normally the mongod executable is downloaded directly from the
      // mongodb web page, however sometimes this behavior is undesired. Some
      // cases are proxy servers, missing internet access, or not wanting to
      // download executables from untrusted sources.
      //
      // Optional it is possible to download it from a source configured in
      // the environment variable:
      String embeddedMongoDownloadPath = System.getenv("EMBEDDED_MONGO_DOWNLOAD_PATH");

      if (embeddedMongoDownloadPath != null) {
        downloadConfigBuilder.downloadPath(
            new SameDownloadPathForEveryDistribution(embeddedMongoDownloadPath));
      }

      return downloadConfigBuilder.build();
    }
  }

  private static MongodStarter ensureMongodStarter() {
    return LazyHolder.INSTANCE;
  }

  private final boolean enableScripting;
  private final IFeatureAwareVersion version;
  private final long timeoutMs;
  private final String username;
  private final String password;
  private final String database;

  private MongodConfig mongodConfig;
  private MongodExecutable mongodExecutable;

  private volatile boolean started;

  StartLocalMongoDbRule(
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

  @Override
  public String getHost() {
    return mongodConfig.net().getBindIp() + ":" + mongodConfig.net().getPort();
  }

  @Override
  public String getDatabase() {
    return database;
  }

  @Override
  public IFeatureAwareVersion getVersion() {
    return version;
  }

  @Override
  public MongoClient createClient() {
    return new MongoClient(
        ServerAddressHelper.createServerAddress(getHost()),
        MongoCredential.createCredential(username, database, password.toCharArray()),
        MongoClientOptions.builder().build());
  }

  @Override
  protected void before() {
    startMongo();
  }

  @Override
  protected void after() {
    stopMongo();
  }

  private void startMongo() {
    if (started) {
      return;
    }

    try {
      ImmutableMongodConfig.Builder mongodConfigBuilder =
          MongodConfig.builder()
              .version(version)
              .net(
                  new Net(
                      Network.getLocalHost().getHostName(), Network.getFreeServerPort(), false));

      if (!enableScripting) {
        mongodConfigBuilder.putArgs("--noscripting", "");
      }

      mongodConfig = mongodConfigBuilder.build();

      mongodExecutable = ensureMongodStarter().prepare(mongodConfig);
      mongodExecutable.start();

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

      try (MongoClient mongoClient = new MongoClient(getHost(), options)) {
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

  private void stopMongo() {
    if (started && mongodExecutable != null) {
      mongodExecutable.stop();
      started = false;
    }
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
