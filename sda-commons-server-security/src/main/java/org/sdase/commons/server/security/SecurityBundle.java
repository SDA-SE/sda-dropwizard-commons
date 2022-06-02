package org.sdase.commons.server.security;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.security.filter.WebSecurityApiOnlyHeaderFilter;
import org.sdase.commons.server.security.filter.WebSecurityFrontendSupportHeaderFilter;
import org.sdase.commons.server.security.handler.ObscuringErrorHandler;
import org.sdase.commons.server.security.validation.BufferLimitsAdvice;
import org.sdase.commons.server.security.validation.CustomErrorHandlerSecurityAdvice;
import org.sdase.commons.server.security.validation.HttpConnectorSecurityAdvice;
import org.sdase.commons.server.security.validation.ServerFactorySecurityAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ConfiguredBundle} that enforces common rules for secure REST applications.
 *
 * <p>This bundle helps to avoid risks identified in the security guide <cite>"Dropwizard
 * Härtungsmaßnahmen"</cite> by <em>Timo Pagel</em>. Risk management is delegated to other classes,
 * that are initialized or invoked from this bundle:
 *
 * <ul>
 *   <li>{@link ServerFactorySecurityAdvice}
 *   <li>{@link HttpConnectorSecurityAdvice}
 *   <li>{@link CustomErrorHandlerSecurityAdvice}
 * </ul>
 */
public class SecurityBundle<T extends Configuration> implements ConfiguredBundle<T> {

  private static final Logger LOG = LoggerFactory.getLogger(SecurityBundle.class);

  public static Builder builder() {
    return new Builder();
  }

  private Bootstrap<?> bootstrap;

  private final boolean disableBufferLimitValidation;

  private final boolean supportFrontend;

  /**
   * Use {@code SecurityBundle.builder().build();}
   *
   * @param disableBufferLimitValidation if buffer limit violations should only produce a log
   *     instead of failing
   */
  private SecurityBundle(boolean disableBufferLimitValidation, boolean supportFrontend) {
    this.disableBufferLimitValidation = disableBufferLimitValidation;
    this.supportFrontend = supportFrontend;
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    this.bootstrap = bootstrap;
  }

  @Override
  public void run(T configuration, Environment environment) {
    ServerFactory serverFactory = configuration.getServerFactory();
    new ServerFactorySecurityAdvice(serverFactory).applySecureConfiguration();
    new HttpConnectorSecurityAdvice(serverFactory).applySecureConfiguration();
    new CustomErrorHandlerSecurityAdvice(serverFactory, this.bootstrap).applySecureConfiguration();
    new BufferLimitsAdvice(serverFactory, disableBufferLimitValidation).applySecureConfiguration();

    environment.getApplicationContext().setErrorHandler(createNewErrorHandler(environment));
    environment.getAdminContext().setErrorHandler(createNewErrorHandler(environment));
    if (supportFrontend) {
      LOG.info(
          "Content-Security-Policy headers are configured to support frontends. "
              + "Services that only serve APIs don't enable this feature.");
      environment.jersey().register(WebSecurityFrontendSupportHeaderFilter.class);
    } else {
      environment.jersey().register(WebSecurityApiOnlyHeaderFilter.class);
    }
  }

  private ObscuringErrorHandler createNewErrorHandler(Environment environment) {
    return new ObscuringErrorHandler(environment.getObjectMapper());
  }

  public static class Builder {

    private boolean disableBufferLimitValidation;
    private boolean supportFrontend;

    /**
     * Switches from suppressing the application start to a warn logging for violated buffer limits.
     * In rare cases an application might need to increase the default limits and therefore has to
     * disable strict validation. This option should be used with care.
     *
     * @return this builder instance
     */
    public Builder disableBufferLimitValidation() {
      this.disableBufferLimitValidation = true;
      return this;
    }

    /**
     * If a service is configured with frontend support, the {@code Content-Security-Policy} header
     * allows the same domain as source for scripts, images, styles and fonts. Otherwise, only API
     * endpoints can be served and {@code Content-Security-Policy} does not allow any sources.
     *
     * @return this builder instance
     * @see <a href="https://en.wikipedia.org/wiki/Content_Security_Policy">CSP header</a>
     */
    public Builder withFrontendSupport() {
      this.supportFrontend = true;
      return this;
    }

    public SecurityBundle<Configuration> build() {
      return new SecurityBundle<>(disableBufferLimitValidation, supportFrontend);
    }
  }
}
