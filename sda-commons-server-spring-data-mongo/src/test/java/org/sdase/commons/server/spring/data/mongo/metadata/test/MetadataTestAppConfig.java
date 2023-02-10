package org.sdase.commons.server.spring.data.mongo.metadata.test;

import io.dropwizard.Configuration;
import org.sdase.commons.server.spring.data.mongo.MongoConfiguration;

public class MetadataTestAppConfig extends Configuration {

  private MongoConfiguration mongo = new MongoConfiguration();

  public MongoConfiguration getMongo() {
    return mongo;
  }

  public MetadataTestAppConfig setMongo(MongoConfiguration mongo) {
    this.mongo = mongo;
    return this;
  }
}
