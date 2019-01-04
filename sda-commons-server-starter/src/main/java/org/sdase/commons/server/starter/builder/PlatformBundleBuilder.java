package org.sdase.commons.server.starter.builder;

import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import org.sdase.commons.server.starter.SdaPlatformBundle;

/**
 * The final builder that is able to configure all the optional settings.
 * @param <C>
 */
public interface PlatformBundleBuilder<C extends Configuration> extends
      CorsCustomizer<C>,
      SecurityCustomizer<C>,
      JacksonCustomizer<C> {

   /**
    * <p>
    *    Builds the configured {@link SdaPlatformBundle} which must be added to the
    *    {@link Bootstrap} in {@link io.dropwizard.Application#initialize(Bootstrap)}.
    * </p>
    *
    * @return the configured {@link SdaPlatformBundle}
    */
   SdaPlatformBundle<C> build();

}
