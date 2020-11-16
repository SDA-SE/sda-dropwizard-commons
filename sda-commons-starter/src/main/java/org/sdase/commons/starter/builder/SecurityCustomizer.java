package org.sdase.commons.starter.builder;

import io.dropwizard.Configuration;

public interface SecurityCustomizer<C extends Configuration> {

  /**
   * Switches from suppressing the application start to a warn logging for violated buffer limits.
   * In rare cases an application might need to increase the default limits and therefore has to
   * disable strict validation. This option should be used with care.
   *
   * @return this builder instance
   */
  PlatformBundleBuilder<C> disableBufferLimitValidationSecurityFeature();
}
