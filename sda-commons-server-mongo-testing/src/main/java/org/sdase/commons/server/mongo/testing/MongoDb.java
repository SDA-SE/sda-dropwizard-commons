package org.sdase.commons.server.mongo.testing;

import static de.flapdoodle.embed.mongo.distribution.Version.Main.V4_4;
import static java.util.concurrent.TimeUnit.MINUTES;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;
import org.apache.commons.lang3.SystemUtils;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;
import org.sdase.commons.server.dropwizard.bundles.SystemPropertyAndEnvironmentLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit Test rule for running a MongoDB instance alongside the (integration) tests. Can be
 * configured with custom user credentials and database name. Use {@link #getHosts()} to retrieve
 * the hosts to connect to.
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
public interface MongoDb {

  /**
   * {@value} is the name of the environment variable that may hold a <a
   * href="https://docs.mongodb.com/manual/reference/connection-string/">MongoDB Connection
   * String</a> of the database used in tests instead of starting a dedicated instance.
   */
  String OVERRIDE_MONGODB_CONNECTION_STRING_SYSTEM_PROPERTY_NAME = "TEST_MONGODB_CONNECTION_STRING";

  /**
   * @return the hostname and port that can be used to connect to the database. The result may
   *     contain a comma separated list of hosts as in the MongoDB Connection String
   */
  String getHosts();

  /** @return the username that must be used to connect to the database. */
  String getUsername();

  /** @return the password that must be used to connect to the database. */
  String getPassword();

  /** @return the initialized database */
  String getDatabase();

  /** @return the MongoDB options String without leading question mark */
  String getOptions();

  /**
   * Creates a MongoClient that is connected to the database. The caller is responsible for closing
   * the connection.
   *
   * @return A MongoClient
   */
  MongoClient createClient();

  /** @return the version of the MongoDB instance which is associated with this MongoDbRule */
  default String getServerVersion() {
    try (MongoClient client = createClient()) {
      return client
          .getDatabase(getDatabase())
          .runCommand(new BsonDocument("buildinfo", new BsonString("")))
          .get("version")
          .toString();
    }
  }

  /**
   * Removes all documents from the database passed during construction. Keeps the collections and
   * indices on the collections.
   */
  default void clearCollections() {
    try (MongoClient client = createClient()) {
      MongoDatabase db = client.getDatabase(getDatabase());

      Iterable<String> collectionNames = db.listCollectionNames();
      collectionNames.forEach(n -> db.getCollection(n).deleteMany(new Document()));
    }
  }

  /**
   * Removes all collections and documents from the database passed during construction. Take care
   * that this also removes all indices from collections.
   */
  default void clearDatabase() {
    try (MongoClient client = createClient()) {
      client.dropDatabase(getDatabase());
    }
  }

  abstract class Builder<T extends MongoDb> {

    public static final Version.Main DEFAULT_VERSION = V4_4;
    public static final Version.Main WINDOWS_VERSION = DEFAULT_VERSION;

    protected static final long DEFAULT_TIMEOUT_MS = MINUTES.toMillis(1L);

    static final String DEFAULT_USER = "dbuser";
    static final String DEFAULT_PASSWORD = "sda123"; // NOSONAR
    static final String DEFAULT_DATABASE = "default_db";

    private static final Logger LOG = LoggerFactory.getLogger(Builder.class);

    protected String mongoDbUrlOverride =
        new SystemPropertyAndEnvironmentLookup()
            .lookup(OVERRIDE_MONGODB_CONNECTION_STRING_SYSTEM_PROPERTY_NAME);

    protected IFeatureAwareVersion version;
    protected Long timeoutInMillis;
    protected String username;
    protected String password;
    protected String database;
    protected boolean scripting = false;

    /**
     * Configure the username that can be used to connect to the MongoDB instance.
     *
     * @param username the username
     * @return a builder instance for further configuration
     */
    public Builder<T> withUsername(String username) {
      this.username = username;
      return this;
    }

    /**
     * Configure the password that can be used to connect to the MongoDB instance.
     *
     * @param password the password
     * @return a builder instance for further configuration
     */
    public Builder<T> withPassword(String password) {
      this.password = password;
      return this;
    }

    /**
     * Configure the database that can be used to connect to the MongoDB instance.
     *
     * @param database the database
     * @return a builder instance for further configuration
     */
    public Builder<T> withDatabase(String database) {
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
    public Builder<T> withVersion(IFeatureAwareVersion version) {
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
    public Builder<T> withTimeoutInMillis(long timeoutInMillis) {
      this.timeoutInMillis = timeoutInMillis;
      return this;
    }

    /**
     * Allows to enable scripting using JavaScript, which is disabled by default. Avoid this option,
     * as it expose your application to security risks.
     *
     * @return a builder instance for further configuration
     */
    public Builder<T> enableScripting() {
      this.scripting = true;
      return this;
    }

    protected IFeatureAwareVersion determineMongoDbVersion() {
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

    protected long getTimeoutMs() {
      return timeoutInMillis == null || timeoutInMillis < 1L ? DEFAULT_TIMEOUT_MS : timeoutInMillis;
    }

    public abstract T build();
  }
}
