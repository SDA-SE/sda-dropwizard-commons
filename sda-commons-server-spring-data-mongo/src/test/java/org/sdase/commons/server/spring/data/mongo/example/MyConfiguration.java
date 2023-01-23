package org.sdase.commons.server.spring.data.mongo.example;

import io.dropwizard.Configuration;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.sdase.commons.server.spring.data.mongo.MongoConfiguration;
import org.sdase.commons.shared.certificates.ca.CaCertificateConfiguration;

public class MyConfiguration extends Configuration {

  @Valid @NotNull private MongoConfiguration springDataMongo = new MongoConfiguration();

  private CaCertificateConfiguration config = new CaCertificateConfiguration();

  public MongoConfiguration getSpringDataMongo() {
    return springDataMongo;
  }

  public MyConfiguration setSpringDataMongo(MongoConfiguration springDataMongo) {
    this.springDataMongo = springDataMongo;
    return this;
  }

  public CaCertificateConfiguration getConfig() {
    return config;
  }
}
