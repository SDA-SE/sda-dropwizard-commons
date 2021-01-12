package org.sdase.commons.server.testing;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import org.sdase.commons.server.testing.builder.DropwizardBuilders.ConfigurationBuilder;
import org.sdase.commons.server.testing.builder.DropwizardExtensionBuilders.CustomizationBuilder;

/**
 * A helper that creates a {@link DropwizardAppExtension} with configuration programmatically.
 *
 * @param <C> the type of the {@link Configuration} used by the {@link Application}
 * @param <A> the type of the {@link Application} that bootstraps the service
 */
public class DropwizardExtensionHelper<C extends Configuration, A extends Application<C>>
    extends DropwizardHelper<C, A> implements CustomizationBuilder<C> {

  public DropwizardExtensionHelper(Class<A> appClass) {
    super(appClass);
  }

  /**
   * Provides a builder that is able to create a {@link DropwizardExtensionHelper} using
   * programmatic custom configuration without the need for a configuration yaml file.
   *
   * @param appClass the {@link Application} class that bootstraps the service
   * @param <C1> the type of the {@link Configuration} used by the {@link Application}
   * @param <A1> the type of the {@link Application} that bootstraps the service
   * @return a builder for configuration of the app
   */
  public static <C1 extends Configuration, A1 extends Application<C1>>
      ConfigurationBuilder<C1> dropwizardTestAppFrom(Class<A1> appClass) {
    return new DropwizardExtensionHelper<>(appClass);
  }

  @Override
  public DropwizardAppExtension<C> build() {
    return new DropwizardAppExtension<>(appClass, configurationBuilder.build());
  }
}
