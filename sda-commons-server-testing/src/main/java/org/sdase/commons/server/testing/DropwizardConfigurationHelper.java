package org.sdase.commons.server.testing;

import io.dropwizard.Configuration;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import org.sdase.commons.server.testing.builder.ConfigurationBuilders.CustomizationBuilder;
import org.sdase.commons.server.testing.builder.ConfigurationBuilders.PortBuilder;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A helper that creates a {@link Configuration} programmatically.
 *
 * @param <C> the type of the {@link Configuration} used by an {@link io.dropwizard.Application}
 */
public class DropwizardConfigurationHelper<C extends Configuration> implements
      PortBuilder<C>,
      CustomizationBuilder<C> {

   private C configuration;

   /**
    *
    * @param configurationSupplier a supplier that creates the initial {@link Configuration}, e.g.
    *                              {@code MyAppConfig::new}
    * @param <C1> the type of the {@link Configuration} used by an {@link io.dropwizard.Application}
    * @return a builder for programmatic configuration
    */
   public static <C1 extends Configuration> PortBuilder<C1> configFrom(Supplier<C1> configurationSupplier) {
      return new DropwizardConfigurationHelper<>(configurationSupplier.get());
   }

   private DropwizardConfigurationHelper(C configuration) {
      this.configuration = configuration;
   }

   @Override
   public CustomizationBuilder<C> withConfigurationModifier(Consumer<C> configurationCustomizer) {
      customize(configurationCustomizer);
      return this;
   }

   @Override
   public CustomizationBuilder<C> withRootPath(String rootPath) {
      applyRootPath(rootPath);
      return this;
   }

   @Override
   public CustomizationBuilder<C> withRandomPorts() {
      applyApplicationPort(0);
      applyAdminPort(0);
      return this;
   }

   @Override
   public CustomizationBuilder<C> withPorts(int applicationPort, int adminPort) {
      applyApplicationPort(applicationPort);
      applyAdminPort(adminPort);
      return this;
   }

   @Override
   public C build() {
      return configuration;
   }

   private void applyApplicationPort(int applicationPort) {
      DefaultServerFactory serverFactory = (DefaultServerFactory) configuration.getServerFactory();
      serverFactory.getApplicationConnectors().stream()
            .filter(c -> c instanceof HttpConnectorFactory)
            .map(c -> (HttpConnectorFactory) c)
            .forEach(c -> c.setPort(applicationPort));
   }

   private void applyAdminPort(int adminPort) {
      DefaultServerFactory serverFactory = (DefaultServerFactory) configuration.getServerFactory();
      serverFactory.getAdminConnectors().stream()
            .filter(c -> c instanceof HttpConnectorFactory)
            .map(c -> (HttpConnectorFactory) c)
            .forEach(c -> c.setPort(adminPort));
   }

   private void applyRootPath(String rootPath) {
      DefaultServerFactory serverFactory = (DefaultServerFactory) configuration.getServerFactory();
      serverFactory.setJerseyRootPath(rootPath);
   }

   private void customize(Consumer<C> customizer) {
      customizer.accept(configuration);
   }

}
