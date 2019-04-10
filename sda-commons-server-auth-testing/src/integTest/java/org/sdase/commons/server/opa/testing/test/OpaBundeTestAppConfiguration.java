package org.sdase.commons.server.opa.testing.test;

import io.dropwizard.Configuration;
import org.sdase.commons.server.opa.config.OpaConfig;

public class OpaBundeTestAppConfiguration extends Configuration {

  private OpaConfig opa = new OpaConfig();

  public OpaConfig getOpa() {
    return opa;
  }

  public OpaBundeTestAppConfiguration setOpa(
      OpaConfig opa) {
    this.opa = opa;
    return this;
  }
}
