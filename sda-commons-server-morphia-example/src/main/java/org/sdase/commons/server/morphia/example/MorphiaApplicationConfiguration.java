package org.sdase.commons.server.morphia.example;

import io.dropwizard.Configuration;
import org.sdase.commons.server.morphia.MongoConfiguration;

/** Example configuration for an application that uses mongo/morphia. */
public class MorphiaApplicationConfiguration extends Configuration {

  /** Configuration object from the morphia bundle: @{@link MongoConfiguration} */
  private MongoConfiguration mongo = new MongoConfiguration();

  public MongoConfiguration getMongo() {
    return mongo;
  }

  public void setMongo(MongoConfiguration mongo) {
    this.mongo = mongo;
  }
}
