package org.sdase.commons.starter.builder;

import io.dropwizard.Configuration;
import org.sdase.commons.server.auth.config.AuthConfigProvider;
import org.sdase.commons.server.cors.CorsConfigProvider;
import org.sdase.commons.server.opa.config.OpaConfigProvider;
import org.sdase.commons.starter.SdaPlatformBundle;
import org.sdase.commons.starter.SdaPlatformConfiguration;
import org.sdase.commons.starter.builder.CustomConfigurationProviders.AuthConfigProviderBuilder;
import org.sdase.commons.starter.builder.CustomConfigurationProviders.ConsumerTokenConfigBuilder;

public interface InitialPlatformBundleBuilder {

  /**
   * Start an application that uses the {@link SdaPlatformConfiguration} as base of it's
   * configuration file.
   *
   * @return the builder instance
   */
  ConsumerTokenConfigBuilder<SdaPlatformConfiguration> usingSdaPlatformConfiguration();

  /**
   * Start an application that uses a customized configuration which extends the {@link
   * SdaPlatformConfiguration} as base of it's configuration file.
   *
   * <p>The method automatically enables OPA authorization and sets therefore the authorization
   * configuration via {@link AuthConfigProviderBuilder#withOpaAuthorization(AuthConfigProvider,
   * OpaConfigProvider)} and the cors configuration via {@link
   * org.sdase.commons.starter.builder.CustomConfigurationProviders.CorsConfigProviderBuilder#withCorsConfigProvider(CorsConfigProvider)}
   *
   * @param configurationClass - the customized configuration of the application
   * @return the builder instance
   */
  <C extends SdaPlatformConfiguration> ConsumerTokenConfigBuilder<C> usingSdaPlatformConfiguration(
      Class<C> configurationClass);

  /**
   * Start an application that uses a custom configuration has to define providers for the
   * configurations required by the {@link SdaPlatformBundle}.
   *
   * @param configurationClass the class that stores the configuration
   * @param <C> the type of the applications configuration class
   * @return the builder instance
   */
  <C extends Configuration> AuthConfigProviderBuilder<C> usingCustomConfig(
      Class<C> configurationClass);
}
