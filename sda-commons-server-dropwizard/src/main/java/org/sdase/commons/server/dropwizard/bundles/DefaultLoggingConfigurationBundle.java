package org.sdase.commons.server.dropwizard.bundles;

import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.dropwizard.logging.ConsoleAppenderInjectorSourceProvider;

/**
 * <p>
 * The {@code DefaultLoggingConfigurationBundle} allows to configure the console
 * logger with the settings desired by the SDA.
 * </p>
 */
public class DefaultLoggingConfigurationBundle implements Bundle {
   public static Builder builder() {
      return new Builder();
   }

   @Override
   public void initialize(Bootstrap<?> bootstrap) {
      bootstrap
            .setConfigurationSourceProvider(
                  new ConsoleAppenderInjectorSourceProvider(bootstrap.getConfigurationSourceProvider()));
   }

   @Override
   public void run(Environment environment) {
      // nothing to run
   }

   public static class Builder {
      public DefaultLoggingConfigurationBundle build() {
         return new DefaultLoggingConfigurationBundle();
      }
   }
}
