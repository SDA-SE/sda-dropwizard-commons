package org.sdase.commons.server.mongo.testing;

import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
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

  private final StartLocalMongoDb startLocalMongoDb;

  StartLocalMongoDbRule(
      String username,
      String password,
      String database,
      boolean enableScripting,
      IFeatureAwareVersion version,
      long timeoutMs) {

    startLocalMongoDb =
        new StartLocalMongoDb(username, password, database, enableScripting, version, timeoutMs);
  }

  @Override
  protected void before() {
    startLocalMongoDb.startMongo();
  }

  @Override
  protected void after() {
    startLocalMongoDb.stopMongo();
  }

  @Override
  public String getDatabase() {
    return startLocalMongoDb.database;
  }

  @Override
  public String getHosts() {
    return startLocalMongoDb.getHosts();
  }

  @Override
  public String getUsername() {
    return startLocalMongoDb.username;
  }

  @Override
  public String getPassword() {
    return startLocalMongoDb.password;
  }

  @Override
  public String getOptions() {
    return "";
  }

  @Override
  public String getConnectionString() {
    return startLocalMongoDb.getConnectionString();
  }

  /**
   * @return the version of the MongoDB instance which is associated with this MongoDbRule
   * @deprecated because this is specific to flap doodle, use {@link #getServerVersion()}
   */
  @Override
  @Deprecated
  public IFeatureAwareVersion getVersion() {
    return startLocalMongoDb.version;
  }

  @Override
  public MongoClient createClient() {
    return startLocalMongoDb.createClient();
  }
}
