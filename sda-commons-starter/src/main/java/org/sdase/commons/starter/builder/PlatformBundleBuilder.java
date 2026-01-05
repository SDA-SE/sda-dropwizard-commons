package org.sdase.commons.starter.builder;

import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import org.sdase.commons.starter.SdaPlatformBundle;

/**
 * The final builder that is able to configure all the optional settings.
 *
 * @param <C> the type of the applications configuration class
 */
public interface PlatformBundleBuilder<C extends Configuration>
    extends CorsCustomizer<C>,
        SecurityCustomizer<C>,
        JacksonCustomizer<C>,
        CustomConfigurationProviders.PrometheusConfigProviderBuilder<C> {

  /**
   * Builds the configured {@link SdaPlatformBundle} which must be added to the {@link Bootstrap} in
   * {@link io.dropwizard.core.Application#initialize(Bootstrap)}.
   *
   * @return the configured {@link SdaPlatformBundle}
   */
  SdaPlatformBundle<C> build();
}
