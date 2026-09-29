package org.sdase.commons.server.dropwizard.bundles;

import io.dropwizard.core.Configuration;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.sdase.commons.server.dropwizard.filter.TraceTokenServerFilter;

public class TraceTokenBundle implements ConfiguredBundle<Configuration> {

  // TODO next-major consider this constructor as private and remove when deprecated subclass is
  // removed
  protected TraceTokenBundle() {}

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // nothing to initialize
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    TraceTokenServerFilter filter = new TraceTokenServerFilter();
    environment.jersey().register(filter);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    public TraceTokenBundle build() {
      return new TraceTokenBundle();
    }
  }
}
