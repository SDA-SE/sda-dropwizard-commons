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
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.Defaults;
import de.flapdoodle.embed.mongo.config.ImmutableMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.packageresolver.Command;
import de.flapdoodle.embed.process.extract.DirectoryAndExecutableNaming;
import de.flapdoodle.embed.process.extract.UUIDTempNaming;
import de.flapdoodle.embed.process.io.directories.*;
import de.flapdoodle.embed.process.runtime.Network;
import de.flapdoodle.embed.process.store.ExtractedArtifactStore;
import de.flapdoodle.embed.process.store.IArtifactStore;
import de.flapdoodle.embed.process.store.ImmutableExtractedArtifactStore;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.lang3.SystemUtils;
import org.bson.BsonDocument;
import org.bson.BsonString;

public class StartLocalMongoDb {

  private static MongodStarter ensureMongodStarter() {
    return LazyHolder.INSTANCE;
  }

  private final boolean enableScripting;
  protected final IFeatureAwareVersion version;

  private MongodConfig mongodConfig;
  private MongodExecutable mongodExecutable;

  private volatile boolean started;

  private final long timeoutMs;
  protected final String username;
  protected final String password;
  protected final String database;

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

  protected void stopMongo() {
    if (started && mongodExecutable != null) {
      mongodExecutable.stop();
      started = false;
    }
  }

  public String getHosts() {
    return mongodConfig.net().getBindIp() + ":" + mongodConfig.net().getPort();
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
    return "";
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

  // Initialization-on-demand holder idiom
  static class LazyHolder {
    static final MongodStarter INSTANCE = getMongoStarter();

    private LazyHolder() {}

    private static MongodStarter getMongoStarter() {
      IArtifactStore artifactStore = createArtifactStore(SystemUtils.IS_OS_MAC_OSX);
      return MongodStarter.getInstance(
          Defaults.runtimeConfigFor(Command.MongoD).artifactStore(artifactStore).build());
    }

    static IArtifactStore createArtifactStore(boolean forMacOs) {
      ImmutableExtractedArtifactStore.Builder artifactStoreBuilder =
          ExtractedArtifactStore.builder()
              .from(Defaults.extractedArtifactStoreFor(Command.MongoD))
              .downloadConfig(createDownloadConfig());

      // avoid recurring firewall requests on mac,
      if (forMacOs) {
        artifactStoreBuilder
            .extraction(
                DirectoryAndExecutableNaming.of(
                    new PropertyOrPlatformTempDir(), (prefix, postfix) -> "mongod"))
            .temp(
                DirectoryAndExecutableNaming.of(
                    new PropertyOrPlatformTempDir(), (prefix, postfix) -> "mongod"));
      } else {
        artifactStoreBuilder.extraction(
            DirectoryAndExecutableNaming.builder()
                .directory(new PropertyOrPlatformTempDir())
                .executableNaming(new UUIDTempNaming())
                .build());
      }
      return artifactStoreBuilder.build();
    }
  }
}
