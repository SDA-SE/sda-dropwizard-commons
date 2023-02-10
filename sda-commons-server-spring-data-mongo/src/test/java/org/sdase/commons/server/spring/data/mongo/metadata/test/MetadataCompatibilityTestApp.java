package org.sdase.commons.server.spring.data.mongo.metadata.test;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.spring.data.mongo.SpringDataMongoBundle;
import org.springframework.data.mongodb.core.MongoOperations;

public class MetadataCompatibilityTestApp extends Application<MetadataTestAppConfig> {

  private final SpringDataMongoBundle<MetadataTestAppConfig> springDataMongoBundle =
      SpringDataMongoBundle.builder()
          .withConfigurationProvider(MetadataTestAppConfig::getMongo)
          .withMorphiaCompatibility()
          .build();

  @Override
  public void initialize(Bootstrap<MetadataTestAppConfig> bootstrap) {
    bootstrap.addBundle(springDataMongoBundle);
  }

  @Override
  public void run(MetadataTestAppConfig configuration, Environment environment) {
    // nothing to do
  }

  public MongoOperations getMongoOperations() {
    return springDataMongoBundle.getMongoOperations();
  }
}
