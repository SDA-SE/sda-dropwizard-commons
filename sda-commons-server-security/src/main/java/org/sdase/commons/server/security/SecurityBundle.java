package org.sdase.commons.server.security;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.security.filter.WebSecurityHeaderFilter;
import org.sdase.commons.server.security.handler.ObscuringErrorHandler;
import org.sdase.commons.server.security.validation.BufferLimitsAdvice;
import org.sdase.commons.server.security.validation.CustomErrorHandlerSecurityAdvice;
import org.sdase.commons.server.security.validation.HttpConnectorSecurityAdvice;
import org.sdase.commons.server.security.validation.ServerFactorySecurityAdvice;

/**
 * A {@link io.dropwizard.Bundle} that enforces common rules for secure REST applications.
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

  public static Builder builder() {
    return new Builder();
  }

  private Bootstrap<?> bootstrap;

  private boolean disableBufferLimitValidation;

  /**
   * Use {@code SecurityBundle.builder().build();}
   *
   * @param disableBufferLimitValidation if buffer limit violations should only produce a log
   *     instead of failing
   */
  private SecurityBundle(boolean disableBufferLimitValidation) {
    this.disableBufferLimitValidation = disableBufferLimitValidation;
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
    environment.jersey().register(WebSecurityHeaderFilter.class);
  }

  private ObscuringErrorHandler createNewErrorHandler(Environment environment) {
    return new ObscuringErrorHandler(environment.getObjectMapper());
  }

  public static class Builder {

    private boolean disableBufferLimitValidation = false;

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

    public SecurityBundle<Configuration> build() {
      return new SecurityBundle<>(disableBufferLimitValidation);
    }
  }
}
