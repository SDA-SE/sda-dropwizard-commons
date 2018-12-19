package org.sdase.commons.server.security;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.security.validation.CustomErrorHandlerSecurityAdvice;
import org.sdase.commons.server.security.validation.HttpConnectorSecurityAdvice;
import org.sdase.commons.server.security.validation.ServerFactorySecurityAdvice;

/**
 * <p>
 *    A {@link io.dropwizard.Bundle} that enforces common rules for secure REST applications.
 * </p>
 * <p>
 *    This bundle helps to avoid risks identified in the security guide <quote>"Dropwizard Härtungsmaßnahmen"</quote> by
 *    <em>Timo Pagel</em>. Risk management is delegated to other classes, that are initialized or invoked from this
 *    bundle:
 * </p>
 * <ul>
 *    <li>{@link ServerFactorySecurityAdvice}</li>
 *    <li>{@link HttpConnectorSecurityAdvice}</li>
 *    <li>{@link CustomErrorHandlerSecurityAdvice}</li>
 * </ul>
 */
public class SecurityBundle<T extends Configuration> implements ConfiguredBundle<T> {

   public static Builder builder() {
      return new Builder();
   }

   /**
    * Use {@code SecurityBundle.builder().build();}
    */
   private SecurityBundle() {
      // hide constructor
   }

   @Override
   public void initialize(Bootstrap<?> bootstrap) {
      // nothing to initialize yet
   }

   @Override
   public void run(T configuration, Environment environment) {
      ServerFactory serverFactory = configuration.getServerFactory();
      new ServerFactorySecurityAdvice(serverFactory).applySecureConfiguration();
      new HttpConnectorSecurityAdvice(serverFactory).applySecureConfiguration();
      new CustomErrorHandlerSecurityAdvice(serverFactory, environment).applySecureConfiguration();
   }

   public static class Builder {
      public SecurityBundle<Configuration> build() {
         return new SecurityBundle<>();
      }
   }
}
