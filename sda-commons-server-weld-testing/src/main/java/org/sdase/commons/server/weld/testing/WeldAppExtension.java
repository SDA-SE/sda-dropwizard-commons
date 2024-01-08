package org.sdase.commons.server.weld.testing;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.annotation.Nullable;

/**
 * An App Rule that uses WELD to inject the application class.
 *
 * <p>Example usage:
 *
 * <pre>
 *   &#64;RegisterExtension
 *   static final WeldAppExtension&lt;AppConfiguration%gt APP =
 *       new WeldAppExtension&lt;&gt;(WeldExampleApplication.class, resourceFilePath("test-config.yaml"));
 * </pre>
 */
public class WeldAppExtension<C extends Configuration> extends DropwizardAppExtension<C> {

  public WeldAppExtension(
      Class<? extends Application<C>> applicationClass,
      @Nullable String configPath,
      ConfigOverride... configOverrides) {
    super(new WeldTestSupport<>(applicationClass, configPath, configOverrides));
  }

  public WeldAppExtension(Class<? extends Application<C>> applicationClass, C configuration) {
    super(new WeldTestSupport<>(applicationClass, configuration));
  }
}
