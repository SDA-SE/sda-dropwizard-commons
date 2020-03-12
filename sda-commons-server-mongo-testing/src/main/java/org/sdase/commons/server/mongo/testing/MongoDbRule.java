package org.sdase.commons.server.mongo.testing;

import static de.flapdoodle.embed.mongo.distribution.Version.Main.*;
import static java.lang.Runtime.getRuntime;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

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
import de.flapdoodle.embed.mongo.config.ExtractedArtifactStoreBuilder;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version.Main;
import de.flapdoodle.embed.process.config.store.DownloadConfigBuilder;
import de.flapdoodle.embed.process.config.store.HttpProxyFactory;
import de.flapdoodle.embed.process.config.store.IDownloadConfig;
import de.flapdoodle.embed.process.config.store.IProxyFactory;
import de.flapdoodle.embed.process.config.store.NoProxyFactory;
import de.flapdoodle.embed.process.runtime.Network;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.lang3.SystemUtils;
import org.bson.Document;
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
public class MongoDbRule extends ExternalResource {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // Initialization-on-demand holder idiom
  private static class LazyHolder {
    static final MongodStarter INSTANCE = getMongoStarter();

    private static MongodStarter getMongoStarter() {
      de.flapdoodle.embed.process.store.ExtractedArtifactStoreBuilder artifactStoreBuilder =
          new ExtractedArtifactStoreBuilder()
              .defaults(Command.MongoD)
              .download(createDownloadConfig());
      // avoid recurring firewall requests on mac, see
      // https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo/issues/40#issuecomment-14969151
      if (SystemUtils.IS_OS_MAC_OSX) {
        artifactStoreBuilder.executableNaming((prefix, postfix) -> "mongod");
      }
      return MongodStarter.getInstance(
          new RuntimeConfigBuilder()
              .defaults(Command.MongoD)
              .artifactStore(artifactStoreBuilder)
              .build());
    }

    private static IProxyFactory createProxyFactory() {
      String httpProxy = System.getenv("http_proxy");
      if (httpProxy != null) {
        try {
          URL url = new URL(httpProxy);

          if (url.getUserInfo() != null) {
            configureAuthentication(url);
          }

          return new HttpProxyFactory(url.getHost(), url.getPort());
        } catch (MalformedURLException exception) {
          LOG.error("http_proxy could not be parsed.");
        }
      }
      return new NoProxyFactory();
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

    private static IDownloadConfig createDownloadConfig() {
      DownloadConfigBuilder downloadConfigBuilder =
          new de.flapdoodle.embed.mongo.config.DownloadConfigBuilder()
              .defaultsForCommand(Command.MongoD)
              .proxyFactory(createProxyFactory());

      // Normally the mongod executable is downloaded directly from the
      // mongodb web page, however sometimes this behavior is undesired. Some
      // cases are proxy servers, missing internet access, or not wanting to
      // download executables from untrusted sources.
      //
      // Optional it is possible to download it from a source configured in
      // the environment variable:
      String embeddedMongoDownloadPath = System.getenv("EMBEDDED_MONGO_DOWNLOAD_PATH");

      if (embeddedMongoDownloadPath != null) {
        downloadConfigBuilder.downloadPath(embeddedMongoDownloadPath);
      }

      return downloadConfigBuilder.build();
    }
  }

  private static MongodStarter ensureMongodStarter() {
    return LazyHolder.INSTANCE;
  }

  public static Builder builder() {
    return new Builder();
  }

  private final boolean enableScripting;
  private final IFeatureAwareVersion version;
  private final long timeoutMs;
  private final String username;
  private final String password;
  private final String database;

  private IMongodConfig mongodConfig;
  private MongodExecutable mongodExecutable;

  private volatile boolean started;

  private MongoDbRule(
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

  /**
   * Returns the hostname and port that can be used to connect to the database.
   *
   * @return Hostname with port.
   */
  public String getHost() {
    return mongodConfig.net().getBindIp() + ":" + mongodConfig.net().getPort();
  }

  /** @return the version of the MongoDB instance which is associated with this MongoDbRule */
  public IFeatureAwareVersion getVersion() {
    return version;
  }

  /**
   * Creates a MongoClient that is connected to the database. The caller is responsible for closing
   * the connection.
   *
   * @return A MongoClient
   */
  public MongoClient createClient() {
    return new MongoClient(
        ServerAddressHelper.createServerAddress(getHost()),
        MongoCredential.createCredential(username, database, password.toCharArray()),
        MongoClientOptions.builder().build());
  }

  /**
   * Removes all documents from the database passed during construction. Keeps the collections and
   * indices on the collections.
   */
  public void clearCollections() {
    try (MongoClient client = createClient()) {
      MongoDatabase db = client.getDatabase(database);

      Iterable<String> collectionNames = db.listCollectionNames();
      collectionNames.forEach(n -> db.getCollection(n).deleteMany(new Document()));
    }
  }

  /**
   * Removes all collections and documents from the database passed during construction. Take care
   * that this also removes all indices from collections.
   */
  public void clearDatabase() {
    try (MongoClient client = createClient()) {
      client.dropDatabase(database);
    }
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
      MongodConfigBuilder mongodConfigBuilder =
          new MongodConfigBuilder()
              .version(version)
              .net(
                  new Net(
                      Network.getLocalHost().getHostName(), Network.getFreeServerPort(), false));

      if (!enableScripting) {
        mongodConfigBuilder = mongodConfigBuilder.withLaunchArgument("--noscripting");
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

  public static final class Builder {

    public static final Main DEFAULT_VERSION = V3_6;
    public static final Main WINDOWS_VERSION = V4_0;

    private static final long DEFAULT_TIMEOUT_MS = MINUTES.toMillis(1L);

    public static final String DEFAULT_USER = "dbuser";
    public static final String DEFAULT_PASSWORD = "sda123"; // NOSONAR
    public static final String DEFAULT_DATABASE = "default_db";

    private IFeatureAwareVersion version;
    private Long timeoutInMillis;
    private String username = DEFAULT_USER;
    private String password = DEFAULT_PASSWORD; // NOSONAR
    private String database = DEFAULT_DATABASE;
    private boolean scripting = false;

    private Builder() {
      // prevent instantiation
    }

    /**
     * Configure the username that can be used to connect to the MongoDB instance, the default user
     * is "dbuser" ({@link #DEFAULT_USER}).
     *
     * @param username the username
     * @return a builder instance for further configuration
     */
    public Builder withUsername(String username) {
      this.username = username;
      return this;
    }

    /**
     * Configure the password that can be used to connect to the MongoDB instance, the default
     * password is "sda123" ({@link #DEFAULT_PASSWORD}).
     *
     * @param password the password
     * @return a builder instance for further configuration
     */
    public Builder withPassword(String password) {
      this.password = password;
      return this;
    }

    /**
     * Configure the database that can be used to connect to the MongoDB instance, the default
     * database is "default_db" ({@link #DEFAULT_DATABASE}).
     *
     * @param database the database
     * @return a builder instance for further configuration
     */
    public Builder withDatabase(String database) {
      this.database = database;
      return this;
    }

    /**
     * Configure the MongoDB version to start, by default the latest production version is used
     * ({@link #DEFAULT_VERSION}).
     *
     * @param version the version
     * @return a builder instance for further configuration
     */
    public Builder withVersion(IFeatureAwareVersion version) {
      this.version = version;
      return this;
    }

    /**
     * Configures the timeout for database startup, the default value is one minute ({@link
     * #DEFAULT_TIMEOUT_MS}).
     *
     * @param timeoutInMillis the timeout in milliseconds
     * @return a builder instance for further configuration
     */
    public Builder withTimeoutInMillis(long timeoutInMillis) {
      this.timeoutInMillis = timeoutInMillis;
      return this;
    }

    /**
     * Allows to enable scripting using JavaScript, which is disabled by default. Avoid this option,
     * as it expose your application to security risks.
     *
     * @return a builder instance for further configuration
     */
    public Builder enableScripting() {
      this.scripting = true;
      return this;
    }

    private IFeatureAwareVersion determineMongoDbVersion() {
      if (version != null) {
        return version;
      } else if (SystemUtils.IS_OS_WINDOWS) {
        LOG.warn(
            "Using MongoDB {} as any version of MongoDB < 4.x may cause issues on a Windows system",
            WINDOWS_VERSION);
        return WINDOWS_VERSION;
      } else {
        return DEFAULT_VERSION;
      }
    }

    public MongoDbRule build() {
      IFeatureAwareVersion mongoDbVersion = determineMongoDbVersion();
      long t =
          timeoutInMillis == null || timeoutInMillis < 1L ? DEFAULT_TIMEOUT_MS : timeoutInMillis;
      return new MongoDbRule(username, password, database, scripting, mongoDbVersion, t);
    }
  }
}
