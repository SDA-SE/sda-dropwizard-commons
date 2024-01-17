package org.sdase.commons.server.dropwizard.bundles.test;

import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.sdase.commons.server.dropwizard.bundles.DefaultLoggingConfigurationBundle;

public class LoggingTestApp extends Application<Configuration> {
  private Configuration configuration;

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    bootstrap.addBundle(new DefaultLoggingConfigurationBundle());
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    // nothing to run
    this.configuration = configuration;
  }

  public Configuration getConfiguration() {
    return configuration;
  }
}
