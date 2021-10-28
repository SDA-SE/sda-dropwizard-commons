package org.sdase.commons.server.dropwizard.bundles;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.dropwizard.logging.ConsoleAppenderInjectorSourceProvider;

/**
 * The {@code DefaultLoggingConfigurationBundle} allows to configure the console logger with the
 * settings desired by the SDA. Requires that the environment variable {@code ENABLE_JSON_LOGGING}
 * is set to {@code "true"}.
 */
public class DefaultLoggingConfigurationBundle implements ConfiguredBundle<Configuration> {
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    bootstrap.setConfigurationSourceProvider(
        new ConsoleAppenderInjectorSourceProvider(bootstrap.getConfigurationSourceProvider()));
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    // nothing to run
  }

  public static class Builder {
    public DefaultLoggingConfigurationBundle build() {
      return new DefaultLoggingConfigurationBundle();
    }
  }
}
