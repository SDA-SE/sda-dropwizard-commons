package org.sdase.commons.starter.builder;

import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import org.sdase.commons.starter.SdaPlatformBundle;

/**
 * The final builder that is able to configure all the optional settings.
 *
 * @param <C> the type of the applications configuration class
 */
public interface PlatformBundleBuilder<C extends Configuration>
    extends CorsCustomizer<C>, SecurityCustomizer<C>, JacksonCustomizer<C> {

  /**
   * Builds the configured {@link SdaPlatformBundle} which must be added to the {@link Bootstrap} in
   * {@link io.dropwizard.Application#initialize(Bootstrap)}.
   *
   * @return the configured {@link SdaPlatformBundle}
   */
  SdaPlatformBundle<C> build();
}
