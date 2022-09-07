package org.sdase.commons.server.spring.data.mongo.example;

import io.dropwizard.Configuration;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.sdase.commons.server.spring.data.mongo.SpringDataMongoConfiguration;

public class MyConfiguration extends Configuration {

  @Valid @NotNull
  private SpringDataMongoConfiguration springDataMongo = new SpringDataMongoConfiguration();

  public SpringDataMongoConfiguration getSpringDataMongo() {
    return springDataMongo;
  }

  public MyConfiguration setSpringDataMongo(SpringDataMongoConfiguration springDataMongo) {
    this.springDataMongo = springDataMongo;
    return this;
  }
}
