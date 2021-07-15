package org.sdase.commons.server.morphia.example;

import io.dropwizard.Configuration;
import org.sdase.commons.server.morphia.MongoConfiguration;
import org.sdase.commons.shared.certificates.ca.CaCertificateConfiguration;

/** Example configuration for an application that uses mongo/morphia. */
public class MorphiaApplicationConfiguration extends Configuration {

  /** Configuration object from the morphia bundle: @{@link MongoConfiguration} */
  private MongoConfiguration mongo = new MongoConfiguration();

  /** Configuration object from the caCertificates bundle: @{@link CaCertificateConfiguration} */
  private CaCertificateConfiguration caCertificateConfiguration = new CaCertificateConfiguration();

  public CaCertificateConfiguration getCaCertificateConfiguration() {
    return caCertificateConfiguration;
  }

  public void setCaCertificateConfiguration(CaCertificateConfiguration caCertificateConfiguration) {
    this.caCertificateConfiguration = caCertificateConfiguration;
  }

  public MongoConfiguration getMongo() {
    return mongo;
  }

  public void setMongo(MongoConfiguration mongo) {
    this.mongo = mongo;
  }
}
