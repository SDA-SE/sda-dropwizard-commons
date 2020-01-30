package org.sdase.commons.server.healthcheck.example;

import io.dropwizard.Configuration;

@SuppressWarnings("WeakerAccess")
public class HealthExampleConfiguration extends Configuration {

  private String externalServiceUrl;

  public String getExternalServiceUrl() {
    return externalServiceUrl;
  }

  public void setExternalServiceUrl(String externalServiceUrl) {
    this.externalServiceUrl = externalServiceUrl;
  }
}
