package org.sdase.commons.server.security;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.security.validation.CustomErrorHandlerSecurityAdvise;
import org.sdase.commons.server.security.validation.HttpConnectorSecurityAdvise;
import org.sdase.commons.server.security.validation.ServerFactorySecurityAdvise;

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
 *    <li>{@link ServerFactorySecurityAdvise}</li>
 *    <li>{@link HttpConnectorSecurityAdvise}</li>
 *    <li>{@link CustomErrorHandlerSecurityAdvise}</li>
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
      new ServerFactorySecurityAdvise(serverFactory).applySecureConfiguration();
      new HttpConnectorSecurityAdvise(serverFactory).applySecureConfiguration();
      new CustomErrorHandlerSecurityAdvise(serverFactory, environment).applySecureConfiguration();
   }

   public static class Builder {
      public SecurityBundle<Configuration> build() {
         return new SecurityBundle<>();
      }
   }
}
