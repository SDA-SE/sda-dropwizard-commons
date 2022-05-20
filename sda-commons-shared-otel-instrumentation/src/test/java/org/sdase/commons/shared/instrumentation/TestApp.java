package org.sdase.commons.shared.instrumentation;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class TestApp extends Application<Configuration> {
  @Override
  public void run(Configuration configuration, Environment environment) throws Exception {}

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    bootstrap.addBundle(InstrumentationBundle.builder().build());
  }
}
