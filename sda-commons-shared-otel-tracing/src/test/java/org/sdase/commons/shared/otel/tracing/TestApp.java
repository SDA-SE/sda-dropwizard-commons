package org.sdase.commons.shared.otel.tracing;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class TestApp extends Application<Configuration> {
  private final TracingBundle tracingBundle = TracingBundle.builder().build();

  @Override
  public void run(Configuration configuration, Environment environment) {}

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    bootstrap.addBundle(tracingBundle);
  }

  public TracingBundle getTracingBundle() {
    return tracingBundle;
  }
}
