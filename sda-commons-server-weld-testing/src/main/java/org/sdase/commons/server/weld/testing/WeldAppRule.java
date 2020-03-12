package org.sdase.commons.server.weld.testing;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.annotation.Nullable;

/**
 * An App Rule that uses WELD to inject the application class.
 *
 * <p>Example usage:
 *
 * <pre>
 *   &#64;ClassRule
 *   public static final WeldAppRule&lt;AppConfiguration&gt; RULE =
 *       new WeldAppRule&lt;&gt;(Application.class, ResourceHelpers.resourceFilePath("config.yml"));
 * </pre>
 */
public class WeldAppRule<C extends Configuration> extends DropwizardAppRule<C> {

  public WeldAppRule(
      Class<? extends Application<C>> applicationClass,
      @Nullable String configPath,
      ConfigOverride... configOverrides) {
    super(new WeldTestSupport<>(applicationClass, configPath, configOverrides));
  }

  public WeldAppRule(Class<? extends Application<C>> applicationClass, C configuration) {
    super(new WeldTestSupport<>(applicationClass, configuration));
  }
}
