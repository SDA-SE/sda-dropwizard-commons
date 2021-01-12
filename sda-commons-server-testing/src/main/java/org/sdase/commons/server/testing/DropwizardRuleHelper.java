package org.sdase.commons.server.testing;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.sdase.commons.server.testing.builder.DropwizardBuilders.ConfigurationBuilder;
import org.sdase.commons.server.testing.builder.DropwizardRuleBuilders.CustomizationBuilder;

/**
 * A helper that creates a {@link DropwizardAppRule} with configuration programmatically.
 *
 * @param <C> the type of the {@link Configuration} used by the {@link Application}
 * @param <A> the type of the {@link Application} that bootstraps the service
 * @deprecated Prefer the original {@link DropwizardAppRule} and a config file with the following
 *     minimal content:
 *     <pre>{@code
 * # use random ports so that tests can run in parallel
 * # and do not affect each other when one is not shutting down
 * server:
 *   applicationConnectors:
 *   - type: http
 *     port: 0
 *   adminConnectors:
 *   - type: http
 *     port: 0
 * }</pre>
 */
@Deprecated
public class DropwizardRuleHelper<C extends Configuration, A extends Application<C>>
    extends DropwizardHelper<C, A> implements CustomizationBuilder<C> {

  public DropwizardRuleHelper(Class<A> appClass) {
    super(appClass);
  }

  /**
   * Provides a builder that is able to create a {@link DropwizardRuleHelper} using programmatic
   * custom configuration without the need for a configuration yaml file.
   *
   * @param appClass the {@link Application} class that bootstraps the service
   * @param <C1> the type of the {@link Configuration} used by the {@link Application}
   * @param <A1> the type of the {@link Application} that bootstraps the service
   * @return a builder for configuration of the app
   */
  public static <C1 extends Configuration, A1 extends Application<C1>>
      ConfigurationBuilder<C1> dropwizardTestAppFrom(Class<A1> appClass) {
    return new DropwizardRuleHelper<>(appClass);
  }

  @Override
  public DropwizardAppRule<C> build() {
    return new DropwizardAppRule<>(appClass, configurationBuilder.build());
  }
}
