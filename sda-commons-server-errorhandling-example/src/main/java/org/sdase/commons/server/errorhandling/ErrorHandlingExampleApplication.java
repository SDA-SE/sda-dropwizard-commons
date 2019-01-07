package org.sdase.commons.server.errorhandling;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.errorhandling.rest.api.ErrorHandlingExampleEndpoint;
import org.sdase.commons.server.jackson.JacksonConfigurationBundle;

public class ErrorHandlingExampleApplication extends Application<Configuration> {

   public static void main(String [] args) throws Exception {
      new ErrorHandlingExampleApplication().run(args);
   }


   @Override
   public void initialize(Bootstrap bootstrap) {
      bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
      bootstrap.addBundle(JacksonConfigurationBundle.builder().build());
   }

   @Override
   public void run(Configuration configuration, Environment environment)  {
      environment.jersey().register(ErrorHandlingExampleEndpoint.class);
   }

}
