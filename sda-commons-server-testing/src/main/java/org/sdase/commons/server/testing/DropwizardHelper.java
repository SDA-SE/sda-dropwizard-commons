package org.sdase.commons.server.testing;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.sdase.commons.server.testing.builder.ConfigurationBuilders;
import org.sdase.commons.server.testing.builder.DropwizardBuilders.ConfigurationBuilder;
import org.sdase.commons.server.testing.builder.DropwizardBuilders.CustomizationBuilder;
import org.sdase.commons.server.testing.builder.DropwizardBuilders.PortBuilder;

/**
 * A helper that creates a {@link DropwizardAppExtension} with configuration programmatically.
 *
 * @param <C> the type of the {@link Configuration} used by the {@link Application}
 * @param <A> the type of the {@link Application} that bootstraps the service
 */
public class DropwizardHelper<C extends Configuration, A extends Application<C>>
    implements ConfigurationBuilder<C>, PortBuilder<C>, CustomizationBuilder<C> {

  protected final Class<A> appClass;

  public DropwizardHelper(Class<A> appClass) {
    this.appClass = appClass;
  }

  protected ConfigurationBuilders.PortBuilder<C> configurationPortBuilder; // intermediate builder
  protected ConfigurationBuilders.CustomizationBuilder<C> configurationBuilder;

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
}
