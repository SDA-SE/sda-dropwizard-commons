package org.sdase.commons.server.opa.testing.test;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.List;
import org.sdase.commons.server.auth.AuthBundle;
import org.sdase.commons.server.auth.config.AuthConfig;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.opa.OpaBundle;
import org.sdase.commons.server.opa.config.OpaConfig;

public class OpaJwtPrincipalInjectApp extends Application<OpaJwtPrincipalInjectApp.Config> {

  @Override
  public void initialize(Bootstrap<Config> bootstrap) {
    bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
    bootstrap.addBundle(
        AuthBundle.builder()
            .withAuthConfigProvider(Config::getAuth)
            .withExternalAuthorization()
            .build());
    bootstrap.addBundle(OpaBundle.builder().withOpaConfigProvider(Config::getOpa).build());
  }

  @Override
  public void run(Config configuration, Environment environment) {
    environment.jersey().register(new OpaJwtPrincipalEndpoint());
  }

  public static class Constraints {
    private boolean allow;
    private List<String> allowedOwners;

    public boolean isAllow() {
      return allow;
    }

    public Constraints setAllow(boolean allow) {
      this.allow = allow;
      return this;
    }

    public List<String> getAllowedOwners() {
      return allowedOwners;
    }

    public Constraints setAllowedOwners(List<String> allowedOwners) {
      this.allowedOwners = allowedOwners;
      return this;
    }
  }

  public static class Config extends Configuration {
    private AuthConfig auth;
    private OpaConfig opa;

    public AuthConfig getAuth() {
      return auth;
    }

    public Config setAuth(AuthConfig auth) {
      this.auth = auth;
      return this;
    }

    public OpaConfig getOpa() {
      return opa;
    }

    public Config setOpa(OpaConfig opa) {
      this.opa = opa;
      return this;
    }
  }
}
