package org.sdase.commons.shared.certificates.ca;

import io.dropwizard.Configuration;

public class CaCertificateTestConfiguration extends Configuration {

  private CaCertificateConfiguration config = new CaCertificateConfiguration();

  public CaCertificateConfiguration getConfig() {
    return config;
  }

  public CaCertificateTestConfiguration setConfig(CaCertificateConfiguration config) {
    this.config = config;
    return this;
  }
}
