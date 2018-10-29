package com.sdase.commons.server.weld.testing;

import com.sdase.commons.server.weld.internal.WeldSupport;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.jboss.weld.environment.se.WeldContainer;

import javax.annotation.Nullable;

/**
 * <p>
 * Test support rule that uses WELD to inject the application class.
 * </p>
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 *    <code>
 *     &#64;ClassRule
 *     public static final DropwizardAppRule<AppConfiguration> RULE = new DropwizardAppRule<>(
 *         new WeldTestSupport<>(Application.class, ResourceHelpers.resourceFilePath("config.yml")));
 *    </code>
 * </pre>
 */
public class WeldTestSupport<C extends Configuration> extends DropwizardTestSupport<C> {

   private WeldContainer container;

   public WeldTestSupport(Class<? extends Application<C>> applicationClass, @Nullable String configPath,
         ConfigOverride... configOverrides) {
      super(applicationClass, configPath, configOverrides);
   }

   @Override
   public Application<C> newApplication() {
      // DI container setup
      container = WeldSupport.createWeldContainer();

      WeldSupport.initializeCDIProviderIfRequired();

      return container.select(applicationClass).get();
   }

   @Override
   public void after() {
      shutdownWeld();

      super.after();
   }

   private void shutdownWeld() {
      if (container != null) {
         container.shutdown();
         container = null;
      }
   }
}
