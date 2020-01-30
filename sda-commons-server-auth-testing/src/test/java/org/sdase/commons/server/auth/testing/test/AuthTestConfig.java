package org.sdase.commons.server.auth.testing.test;

import io.dropwizard.Configuration;
import org.sdase.commons.server.auth.config.AuthConfig;

public class AuthTestConfig extends Configuration {

  private AuthConfig auth;

  public AuthConfig getAuth() {
    return auth;
  }

  public AuthTestConfig setAuth(AuthConfig auth) {
    this.auth = auth;
    return this;
  }
}
