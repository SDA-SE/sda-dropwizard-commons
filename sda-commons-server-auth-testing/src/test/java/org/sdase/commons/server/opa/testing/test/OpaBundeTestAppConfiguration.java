package org.sdase.commons.server.opa.testing.test;

import io.dropwizard.Configuration;
import org.sdase.commons.server.auth.config.AuthConfig;
import org.sdase.commons.server.opa.config.OpaConfig;

public class OpaBundeTestAppConfiguration extends Configuration {

  private AuthConfig auth = new AuthConfig();

  private OpaConfig opa = new OpaConfig();

  public OpaConfig getOpa() {
    return opa;
  }

  public OpaBundeTestAppConfiguration setOpa(OpaConfig opa) {
    this.opa = opa;
    return this;
  }

  public AuthConfig getAuth() {
    return auth;
  }

  public OpaBundeTestAppConfiguration setAuth(AuthConfig auth) {
    this.auth = auth;
    return this;
  }
}
