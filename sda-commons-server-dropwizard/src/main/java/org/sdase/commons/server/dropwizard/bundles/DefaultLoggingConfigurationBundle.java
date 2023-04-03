package org.sdase.commons.server.dropwizard.bundles;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.Map;
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

  public DefaultLoggingConfigurationBundle() {
    super();
  }

  public DefaultLoggingConfigurationBundle(Map<String, String> additionalFields) {
    this.additionalFields = additionalFields;
  }

  private Map<String, String> additionalFields = null;

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    bootstrap.setConfigurationSourceProvider(
        new ConsoleAppenderInjectorSourceProvider(
            bootstrap.getConfigurationSourceProvider(), additionalFields));
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    // nothing to run
  }

  public static class Builder {
    Map<String, String> additionalFields;

    public Builder withAdditionalFields(Map<String, String> additionalFields) {
      this.additionalFields = additionalFields;
      return this;
    }

    public DefaultLoggingConfigurationBundle build() {
      return new DefaultLoggingConfigurationBundle(additionalFields);
    }
  }
}
