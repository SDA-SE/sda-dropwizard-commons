package org.sdase.commons.server.dropwizard.bundles;

import io.dropwizard.Bundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * The {@code ConfigurationSubstitutionBundle} allows to use placeholders for environment variables
 * in configuration yaml files. It should be added as first bundle in the application.
 *
 * <p>The {@code config.yaml} may contain placeholders that are replaced by the content of
 * environment variables and optional default values:
 *
 * <pre>{@code
 * server:
 *   rootPath: ${ROOT_PATH:/api/*}
 *
 * }</pre>
 *
 * <p>Nested placeholders are supported:
 *
 * <pre>{@code
 * example: ${EXAMPLE_VALUE:-bar-${EXAMPLE_SUFFIX}}
 *
 * }</pre>
 */
public class ConfigurationSubstitutionBundle implements Bundle {

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    bootstrap.setConfigurationSourceProvider(
        new SubstitutingSourceProvider(
            bootstrap.getConfigurationSourceProvider(),
            new EnvironmentVariableSubstitutor(false, true)));
  }

  @Override
  public void run(Environment environment) {
    // nothing that has to be done in the run phase
  }

  public static class Builder {
    public ConfigurationSubstitutionBundle build() {
      return new ConfigurationSubstitutionBundle();
    }
  }
}
