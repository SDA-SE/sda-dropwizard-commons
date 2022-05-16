package org.sdase.commons.shared.instrumentation;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class InstrumentationBundle implements ConfiguredBundle<Configuration> {
  @Override
  public void run(Configuration configuration, Environment environment) {
    // attach the otel agent if found.
    AgentLoader.load();
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // do nothing
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    public InstrumentationBundle build() {
      return new InstrumentationBundle();
    }
  }
}
