package org.sdase.commons.server.auth.testing.test;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.sdase.commons.server.auth.AuthBundle;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;

public class AuthTestApp extends Application<AuthTestConfig> {

  @Override
  public void initialize(Bootstrap<AuthTestConfig> bootstrap) {
    bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
    bootstrap.addBundle(
        AuthBundle.builder()
            .withAuthConfigProvider(AuthTestConfig::getAuth)
            .withAnnotatedAuthorization()
            .build());
  }

  @Override
  public void run(AuthTestConfig configuration, Environment environment) {
    environment.jersey().register(SecureEndPoint.class);
    environment.jersey().register(OpenEndPoint.class);
  }
}
