package org.sdase.commons.server.mongo.testing;

import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit Test extension for running a MongoDB instance alongside the (integration) tests. This
 * instance starts a local mongo db which is the default behavior of {@link MongoDbClassExtension}.
 *
 * @see org.sdase.commons.server.mongo.testing.StartLocalMongoDb
 */
public class StartLocalMongoDbClassExtension extends StartLocalMongoDb
    implements MongoDbClassExtension, AfterAllCallback {

  StartLocalMongoDbClassExtension(
      String username,
      String password,
      String database,
      boolean enableScripting,
      IFeatureAwareVersion version,
      long timeoutMs) {
    super(username, password, database, enableScripting, version, timeoutMs);
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    startMongo();
  }

  @Override
  public void afterAll(ExtensionContext context) {
    stopMongo();
  }
}
