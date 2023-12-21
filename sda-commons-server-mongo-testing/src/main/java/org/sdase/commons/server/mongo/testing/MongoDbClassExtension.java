package org.sdase.commons.server.mongo.testing;

import static org.sdase.commons.server.mongo.testing.MongoDb.Builder.DEFAULT_PASSWORD;
import static org.sdase.commons.server.mongo.testing.MongoDb.Builder.DEFAULT_USER;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;

/**
 * JUnit 5 Extension for running a MongoDB instance alongside the (integration) tests. Can be
 * configured with custom user credentials and database name. Use {@link #getConnectionString()} *
 * to retrieve the connection string.
 *
 * <p>Example usage:
 *
 * <pre>
 * &#64;RegisterExtension
 * &#64;Order(0)
 * static final MongoDbClassExtension MONGO_DB_EXTENSION = MongoDbClassExtension
 *     .builder()
 *     .withDatabase("my_db")
 *     .withUsername("my_user")
 *     .withPassword("my_s3cr3t")
 *     .build();
 * </pre>
 *
 * <p>Normally a MongoDb is started on demand. For connecting to an already running MongoDb set the
 * environment variable {@code TEST_MONGODB_CONNECTION_STRING}.
 */
public interface MongoDbClassExtension extends MongoDb, BeforeAllCallback, AfterAllCallback {

  static Builder builder() {
    return new Builder();
  }

  default String getUsername() {
    return DEFAULT_USER;
  }

  default String getPassword() {
    return DEFAULT_PASSWORD;
  }

  final class Builder extends MongoDb.Builder<MongoDbClassExtension> {

    private Builder() {
      // prevent instantiation
      username = DEFAULT_USER;
      password = DEFAULT_PASSWORD; // NOSONAR Sonar's security hotspot
      database = DEFAULT_DATABASE;
    }

    public MongoDbClassExtension build() {

      if (StringUtils.isNotBlank(mongoDbUrlOverride)) {

        return new UseExistingMongoDbClassExtension(mongoDbUrlOverride);

      } else {

        return new StartLocalMongoDbClassExtension(
            username, password, database, scripting, determineMongoDbVersion(), getTimeoutMs());
      }
    }
  }
}
