package org.sdase.commons.server.testing.builder;

import io.dropwizard.Configuration;
import java.util.function.Consumer;

public interface ConfigurationBuilders {

  interface CustomizationBuilder<C extends Configuration> {

    /**
     * Allows to customize the configuration programmatically.
     *
     * @param configurationCustomizer a consumer that receives the current {@link Configuration} of
     *     type {@code C} to modify it's internals
     * @return a builder for further configuration
     */
    CustomizationBuilder<C> withConfigurationModifier(Consumer<C> configurationCustomizer);

    /**
     * Sets the {@link io.dropwizard.server.DefaultServerFactory#setJerseyRootPath(String) jersey
     * root path}, referenced in the {@code config.yaml} as
     *
     * <pre>
     *   server:
     *     rootPath: /*
     * </pre>
     *
     * @param rootPath the path to set as root of the API
     * @return the builder instance
     */
    CustomizationBuilder<C> withRootPath(String rootPath);

    /**
     * @return the {@link Configuration} of type {@code C} created with this builder
     */
    C build();
  }

  interface PortBuilder<C extends Configuration> {

    /**
     * Configures random application port and the admin port. This settings is equal to setting the
     * connector ports in the configuration yaml to {@code 0}
     *
     * @return the builder instance
     */
    CustomizationBuilder<C> withRandomPorts();

    /**
     * Sets the given ports as fixed ports. This settings is equal to setting the connector ports in
     * the configuration yaml to the given ports.
     *
     * @param applicationPort the port the application connector listens to
     * @param adminPort the port the admin connector listens to
     * @return the builder instance
     */
    CustomizationBuilder<C> withPorts(int applicationPort, int adminPort);
  }
}
