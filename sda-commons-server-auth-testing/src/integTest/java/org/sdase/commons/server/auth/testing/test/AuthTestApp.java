package org.sdase.commons.server.auth.testing.test;

import org.sdase.commons.server.auth.AuthBundle;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class AuthTestApp extends Application<AuthTestConfig> {

   @Override
   public void initialize(Bootstrap<AuthTestConfig> bootstrap) {
      bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
      bootstrap.addBundle(AuthBundle.builder().withAuthConfigProvider(AuthTestConfig::getAuth).build());
   }

   @Override
   public void run(AuthTestConfig configuration, Environment environment) {
      environment.jersey().register(SecureEndPoint.class);
      environment.jersey().register(OpenEndPoint.class);
      environment.jersey().register(RolesAwareEndpoint.class);
   }
}
