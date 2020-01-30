package org.sdase.commons.server.morphia.test;

import io.dropwizard.Configuration;
import org.sdase.commons.server.morphia.MongoConfiguration;

public class Config extends Configuration {
  private MongoConfiguration mongo = new MongoConfiguration();

  public MongoConfiguration getMongo() {
    return mongo;
  }

  public Config setMongo(MongoConfiguration mongo) {
    this.mongo = mongo;
    return this;
  }
}
