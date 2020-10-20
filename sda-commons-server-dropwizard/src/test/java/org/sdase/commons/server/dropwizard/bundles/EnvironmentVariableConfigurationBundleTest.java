package org.sdase.commons.server.dropwizard.bundles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.dropwizard.bundles.test.EnvTestApp;
import org.sdase.commons.server.testing.EnvironmentRule;

public class EnvironmentVariableConfigurationBundleTest {

  public static final EnvironmentRule ENV =
      new EnvironmentRule()
          .setEnv("dw.metrics.frequency", "5d")
          .setEnv("dw.config.my\\.property\\.value", "a")
          .setEnv("dw.config.with..dots..property", "b")
          .setEnv("dw.arrayConfig", "one.property,two.property");

  public static final DropwizardAppRule<EnvTestApp.EnvConfiguration> DW =
      new DropwizardAppRule<>(EnvTestApp.class);

  @ClassRule public static final RuleChain RULE = RuleChain.outerRule(ENV).around(DW);

  @Test
  public void shouldUseValuesFromEnvironmentVariables() {
    EnvTestApp.EnvConfiguration configuration = DW.getConfiguration();

    assertThat(configuration.getMetricsFactory().getFrequency().toSeconds())
        .isEqualTo(5L * 24 * 60 * 60);
    assertThat(configuration.getConfig())
        .containsExactly(entry("my.property.value", "a"), entry("with.dots.property", "b"));
    assertThat(configuration.getArrayConfig()).containsExactly("one.property", "two.property");
  }
}
