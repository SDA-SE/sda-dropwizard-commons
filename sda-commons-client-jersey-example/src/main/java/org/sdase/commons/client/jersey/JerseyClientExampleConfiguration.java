package org.sdase.commons.client.jersey;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import io.dropwizard.core.Configuration;

class JerseyClientExampleConfiguration extends Configuration {

  private String servicea;
  @NotNull @Valid private HttpClientConfiguration configuredClient;

  String getServicea() {
    return servicea;
  }

  public void setServicea(String servicea) {
    this.servicea = servicea;
  }

  public HttpClientConfiguration getConfiguredClient() {
    return configuredClient;
  }

  public void setConfiguredClient(HttpClientConfiguration configuredClient) {
    this.configuredClient = configuredClient;
  }
}
