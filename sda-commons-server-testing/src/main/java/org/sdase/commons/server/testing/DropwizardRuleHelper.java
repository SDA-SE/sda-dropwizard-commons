package org.sdase.commons.server.testing;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.sdase.commons.server.testing.builder.ConfigurationBuilders;
import org.sdase.commons.server.testing.builder.DropwizardRuleBuilders.ConfigurationBuilder;
import org.sdase.commons.server.testing.builder.DropwizardRuleBuilders.CustomizationBuilder;
import org.sdase.commons.server.testing.builder.DropwizardRuleBuilders.PortBuilder;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A helper that creates a {@link DropwizardAppRule} with configuration programmatically.
 *
 * @param <C> the type of the {@link Configuration} used by the {@link Application}
 * @param <A> the type of the {@link Application} that bootstraps the service
 */
public class DropwizardRuleHelper<C extends Configuration, A extends Application<C>>
      implements ConfigurationBuilder<C>, PortBuilder<C>, CustomizationBuilder<C> {

   /**
    * Provides a builder that is able to create a {@link DropwizardRuleHelper} using programmatic custom configuration
    * without the need for a configuration yaml file.
    *
    * @param appClass the {@link Application} class that bootstraps the service
    * @param <C1> the type of the {@link Configuration} used by the {@link Application}
    * @param <A1> the type of the {@link Application} that bootstraps the service
    * @return a builder for configuration of the app
    */
   public static <C1 extends Configuration, A1 extends Application<C1>> ConfigurationBuilder<C1> dropwizardTestAppFrom(Class<A1> appClass) {
      return new DropwizardRuleHelper<>(appClass);
   }

   private Class<A> appClass;

   private ConfigurationBuilders.PortBuilder<C> configurationPortBuilder; // intermediate builder
   private ConfigurationBuilders.CustomizationBuilder<C> configurationBuilder;

   private DropwizardRuleHelper(Class<A> appClass) {
      this.appClass = appClass;
   }

   @Override
   public PortBuilder<C> withConfigFrom(Supplier<C> configurationSupplier) {
      this.configurationPortBuilder = DropwizardConfigurationHelper.configFrom(configurationSupplier);
      return this;
   }

   @Override
   public CustomizationBuilder<C> withRandomPorts() {
      configurationBuilder = configurationPortBuilder.withRandomPorts();
      return this;
   }

   @Override
   public CustomizationBuilder<C> withPorts(int applicationPort, int adminPort) {
      configurationBuilder = configurationPortBuilder.withPorts(applicationPort, adminPort);
      return this;
   }

   @Override
   public CustomizationBuilder<C> withConfigurationModifier(Consumer<C> configurationCustomizer) {
      configurationBuilder = configurationBuilder.withConfigurationModifier(configurationCustomizer);
      return this;
   }

   @Override
   public CustomizationBuilder<C> withRootPath(String rootPath) {
      configurationBuilder = configurationBuilder.withRootPath(rootPath);
      return this;
   }

   @Override
   public DropwizardAppRule<C> build() {
      return new DropwizardAppRule<>(appClass, configurationBuilder.build());
   }
}



