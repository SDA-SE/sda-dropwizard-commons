package org.sdase.commons.server.starter.builder;

import io.dropwizard.Configuration;
import org.sdase.commons.server.starter.SdaPlatformConfiguration;
import org.sdase.commons.server.starter.builder.CustomConfigurationProviders.AuthConfigProviderBuilder;
import org.sdase.commons.server.starter.builder.CustomConfigurationProviders.ConsumerTokenConfigBuilder;

public interface InitialPlatformBundleBuilder {

   /**
    * Start an application that uses the {@link SdaPlatformConfiguration} as base of it's configuration file.
    *
    * @return the builder instance
    */
   ConsumerTokenConfigBuilder<SdaPlatformConfiguration> usingSdaPlatformConfiguration();

   /**
    * Start an application that uses a custom configuration has to define providers for the configurations required by
    * the {@link org.sdase.commons.server.starter.SdaPlatformBundle}.
    *
    * @return the builder instance
    */
   <C extends Configuration> AuthConfigProviderBuilder<C> usingCustomConfig(Class<C> configurationClass);

}
