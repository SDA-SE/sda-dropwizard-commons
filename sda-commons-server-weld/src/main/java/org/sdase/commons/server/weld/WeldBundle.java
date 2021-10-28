package org.sdase.commons.server.weld;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.jboss.weld.environment.servlet.Listener;

/**
 * Dropwizard Bundle that adds a listener for using Injection inside of servlets.
 *
 * <p>The use of the Bundle is optional, but the use of the {@link DropwizardWeldHelper} is
 * required.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * public void initialize(final Bootstrap<AppConfiguration> bootstrap) {
 *   bootstrap.addBundle(new WeldBundle());
 * }
 * }</pre>
 */
public class WeldBundle implements ConfiguredBundle<Configuration> {
  @Override
  public void initialize(final Bootstrap<?> bootstrap) {
    // not implemented
  }

  @Override
  public void run(final Configuration configuration, final Environment environment) {
    environment.getApplicationContext().addEventListener(new Listener());
  }
}
