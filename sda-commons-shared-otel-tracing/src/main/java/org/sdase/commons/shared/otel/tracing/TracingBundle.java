package org.sdase.commons.shared.otel.tracing;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;

public class TracingBundle implements ConfiguredBundle<Configuration> {

  private OpenTelemetry openTelemetry;

  @Override
  public void run(Configuration configuration, Environment environment) {
    // it is recommended to only have one instance
    openTelemetry = GlobalOpenTelemetry.get();
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // do nothing
  }

  /**
   * @return An {@link OpenTelemetry} instance that is configured using the provided environment
   *     variables.
   */
  public OpenTelemetry getOpenTelemetry() {
    if (openTelemetry == null) {
      throw new IllegalStateException(
          "Can not access openTelemetrySDK instance before Application#run(Configuration, Environment).");
    }
    return openTelemetry;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    public TracingBundle build() {
      return new TracingBundle();
    }
  }
}
