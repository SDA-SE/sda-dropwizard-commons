package org.sdase.commons.server.mongo.testing;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit Test extension for connecting to an existing MongoDB instance alongside the (integration)
 * tests..
 *
 * @see org.sdase.commons.server.mongo.testing.UseExistingMongoDb
 */
public class UseExistingMongoDbClassExtension extends UseExistingMongoDb
    implements MongoDbClassExtension {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(UseExistingMongoDbClassExtension.class);

  public UseExistingMongoDbClassExtension(String mongoDbConnectionString) {
    super(mongoDbConnectionString);
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    logConnection();
  }
}
