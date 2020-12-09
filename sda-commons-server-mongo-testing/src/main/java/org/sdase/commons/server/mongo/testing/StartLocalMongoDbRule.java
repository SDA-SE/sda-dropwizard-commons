package org.sdase.commons.server.mongo.testing;

import static java.lang.Runtime.getRuntime;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.sdase.commons.server.mongo.testing.internal.DownloadConfigFactoryUtil.createDownloadConfig;

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
import de.flapdoodle.embed.process.runtime.Network;
import de.flapdoodle.embed.process.store.ExtractedArtifactStore;
import de.flapdoodle.embed.process.store.ImmutableExtractedArtifactStore;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import org.junit.rules.ExternalResource;

/**
 * JUnit Test rule for running a MongoDB instance alongside the (integration) tests. Can be
 * configured with custom user credentials and database name. Use {@link #getHosts()} to retrieve
 * the host to connect to.
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
  public String getHosts() {
    return mongodConfig.net().getBindIp() + ":" + mongodConfig.net().getPort();
  }

  @Override
  public String getDatabase() {
    return database;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getOptions() {
    return "";
  }

  /**
   * @return the version of the MongoDB instance which is associated with this MongoDbRule
   * @deprecated because this is specific to flap doodle, use {@link #getServerVersion()}
   */
  @Override
  @Deprecated
  public IFeatureAwareVersion getVersion() {
    return version;
  }

  @Override
  public MongoClient createClient() {
    return new MongoClient(
        ServerAddressHelper.createServerAddress(getHosts()),
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
