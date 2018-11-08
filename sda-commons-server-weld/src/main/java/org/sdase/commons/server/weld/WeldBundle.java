package org.sdase.commons.server.weld;

import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.jboss.weld.environment.servlet.Listener;

/**
 * <p>
 * Dropwizard Bundle that adds a listener for using Injection inside of
 * servlets.
 * </p>
 * <p>
 * The use of the Bundle is optional, but the use of the
 * <code>DropwizardWeldHelper</code> is required.
 * </p>
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 *     <code>
 *     public void initialize(final Bootstrap<AppConfiguration> bootstrap) {
 *         bootstrap.addBundle(new WeldBundle());
 *     }
 *     </code>
 * </pre>
 */
public class WeldBundle implements Bundle {
   @Override
   public void initialize(final Bootstrap<?> bootstrap) {
      // not implemented
   }

   @Override
   public void run(final Environment environment) {
      environment.getApplicationContext().addEventListener(new Listener());
   }
}
