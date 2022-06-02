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

  /**
   * If a service is configured with frontend support, the {@code Content-Security-Policy} header
   * allows the same domain as source for scripts, images, styles and fonts. Otherwise, only API
   * endpoints can be served and {@code Content-Security-Policy} does not allow any sources.
   *
   * @return this builder instance
   * @see <a href="https://en.wikipedia.org/wiki/Content_Security_Policy">CSP header</a>
   */
  PlatformBundleBuilder<C> withFrontendSupport();
}
