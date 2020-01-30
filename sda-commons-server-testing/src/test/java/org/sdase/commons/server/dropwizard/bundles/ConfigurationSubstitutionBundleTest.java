package org.sdase.commons.server.dropwizard.bundles;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.assertj.core.api.Assertions;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.dropwizard.test.DropwizardApp;
import org.sdase.commons.server.dropwizard.test.DropwizardConfig;
import org.sdase.commons.server.testing.EnvironmentRule;

public class ConfigurationSubstitutionBundleTest {

  @ClassRule
  public static final EnvironmentRule ENV =
      new EnvironmentRule()
          .setEnv("ENV_REPLACEMENT", "valueFromEnv")
          .setEnv("EXAMPLE_SUFFIX", "hello");

  @ClassRule
  public static final DropwizardAppRule<DropwizardConfig> DW =
      new DropwizardAppRule<>(
          DropwizardApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

  @Test
  public void shouldReplaceWithoutDefaultDefined() {
    Assertions.assertThat(DW.getConfiguration().getEnvReplaced()).isEqualTo("valueFromEnv");
  }

  @Test
  public void shouldReplaceWithDefaultDefined() {
    Assertions.assertThat(DW.getConfiguration().getEnvWithDefaultReplaced())
        .isEqualTo("valueFromEnv");
  }

  @Test
  public void shouldUseDefaultIfEnvNotDefined() {
    Assertions.assertThat(DW.getConfiguration().getEnvDefault()).isEqualTo("DEFAULT");
  }

  @Test
  public void shouldKeepPlaceholderIfNoEnvAndNoDefault() {
    Assertions.assertThat(DW.getConfiguration().getEnvMissing()).isEqualTo("${ENV_MISSING}");
  }

  @Test
  public void conditionalConfig() {
    Assertions.assertThat(DW.getConfiguration().getOptionalConfig()).isNotNull();
  }

  @Test
  public void shouldSupportNestedVariables() {
    Assertions.assertThat(DW.getConfiguration().getExample())
        .startsWith("bar-")
        .isEqualTo("bar-hello");
  }
}
