package org.sdase.commons.server.mongo.testing;

import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import org.apache.commons.lang3.StringUtils;
import org.junit.rules.TestRule;

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
public interface MongoDbRule extends MongoDb, TestRule {

  static Builder builder() {
    return new Builder();
  }

  /**
   * @return the hostname and port that can be used to connect to the database. The result may
   *     contain a comma separated list of hosts as in the MongoDB Connection String
   * @deprecated in favor of {@link #getHosts()}
   */
  @Deprecated
  default String getHost() {
    return getHosts();
  }

  /**
   * @return the version of the MongoDB instance which is associated with this MongoDbRule
   * @throws UnsupportedOperationException if the database is not bootstrapped with flap doodle
   * @deprecated because this is specific to flap doodle, use {@link #getServerVersion()}
   */
  @Deprecated
  default IFeatureAwareVersion getVersion() {
    throw new UnsupportedOperationException();
  }

  final class Builder extends MongoDb.Builder<MongoDbRule> {

    /** @deprecated use {@link MongoDbRule#getUsername()} of the actual rule instance */
    @Deprecated public static final String DEFAULT_USER = "dbuser";

    /** @deprecated use {@link MongoDbRule#getPassword()} ()} of the actual rule instance */
    @Deprecated public static final String DEFAULT_PASSWORD = "sda123"; // NOSONAR

    /** @deprecated use {@link MongoDbRule#getDatabase()} of the actual rule instance */
    @Deprecated public static final String DEFAULT_DATABASE = "default_db";

    private Builder() {
      // prevent instantiation
      username = DEFAULT_USER;
      password = DEFAULT_PASSWORD; // NOSONAR
      database = DEFAULT_DATABASE;
    }

    public MongoDbRule build() {
      if (StringUtils.isNotBlank(mongoDbUrlOverride)) {
        return new UseExistingMongoDbRule(mongoDbUrlOverride);
      } else {
        return new StartLocalMongoDbRule(
            username, password, database, scripting, determineMongoDbVersion(), getTimeoutMs());
      }
    }
  }
}
