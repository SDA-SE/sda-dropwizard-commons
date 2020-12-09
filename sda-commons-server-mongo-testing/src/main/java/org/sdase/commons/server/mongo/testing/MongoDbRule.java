package org.sdase.commons.server.mongo.testing;

import static de.flapdoodle.embed.mongo.distribution.Version.Main.V3_6;
import static de.flapdoodle.embed.mongo.distribution.Version.Main.V4_0;
import static java.util.concurrent.TimeUnit.MINUTES;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;
import org.apache.commons.lang3.SystemUtils;
import org.bson.Document;
import org.junit.rules.TestRule;
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
public interface MongoDbRule extends TestRule {

  static Builder builder() {
    return new Builder();
  }

  /**
   * Returns the hostname and port that can be used to connect to the database.
   *
   * @return Hostname with port.
   */
  String getHost();

  /** @return the initialized database */
  String getDatabase();

  /**
   * @return the version of the MongoDB instance which is associated with this MongoDbRule
   */
  IFeatureAwareVersion getVersion();

  /**
   * Creates a MongoClient that is connected to the database. The caller is responsible for closing
   * the connection.
   *
   * @return A MongoClient
   */
  MongoClient createClient();

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

  final class Builder {

    private static final Logger LOG = LoggerFactory.getLogger(Builder.class);

    public static final Version.Main DEFAULT_VERSION = V3_6;
    public static final Version.Main WINDOWS_VERSION = V4_0;

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
      return new StartLocalMongoDbRule(username, password, database, scripting, mongoDbVersion, t);
    }
  }
}
