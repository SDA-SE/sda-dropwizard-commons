package com.sdase.commons.server.auth.testing.test;

import com.sdase.commons.server.auth.config.AuthConfig;
import io.dropwizard.Configuration;

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
