package org.sdase.commons.server.errorhandling;

import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.errorhandling.rest.api.ErrorHandlingExampleEndpoint;
import org.sdase.commons.server.jackson.JacksonConfigurationBundle;

public class ErrorHandlingExampleApplication extends Application<Configuration> {

  public static void main(String[] args) throws Exception {
    new ErrorHandlingExampleApplication().run(args);
  }

  @Override
  public void initialize(Bootstrap bootstrap) {
    bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
    // Jackson Bundle includes error mappers to provide errors in common error structure
    bootstrap.addBundle(JacksonConfigurationBundle.builder().build());
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    // register dummy endpoint to show behavior in tests
    environment.jersey().register(ErrorHandlingExampleEndpoint.class);
  }
}
