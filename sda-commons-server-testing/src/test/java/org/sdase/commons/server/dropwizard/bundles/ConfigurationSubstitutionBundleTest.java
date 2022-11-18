package org.sdase.commons.server.dropwizard.bundles;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.dropwizard.test.DropwizardApp;
import org.sdase.commons.server.dropwizard.test.DropwizardConfig;
import org.sdase.commons.server.testing.SystemPropertyClassExtension;

class ConfigurationSubstitutionBundleTest {

  @RegisterExtension
  @Order(0)
  static final SystemPropertyClassExtension SYS =
      new SystemPropertyClassExtension()
          .setProperty("ENV_REPLACEMENT", "valueFromEnv")
          .setProperty("EXAMPLE_SUFFIX", "hello");

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<DropwizardConfig> DW =
      new DropwizardAppExtension<>(
          DropwizardApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

  @Test
  void shouldReplaceWithoutDefaultDefined() {
    assertThat(DW.getConfiguration().getEnvReplaced()).isEqualTo("valueFromEnv");
  }

  @Test
  void shouldReplaceWithDefaultDefined() {
    assertThat(DW.getConfiguration().getEnvWithDefaultReplaced()).isEqualTo("valueFromEnv");
  }

  @Test
  void shouldUseDefaultIfEnvNotDefined() {
    assertThat(DW.getConfiguration().getEnvDefault()).isEqualTo("DEFAULT");
  }

  @Test
  void shouldKeepPlaceholderIfNoEnvAndNoDefault() {
    assertThat(DW.getConfiguration().getEnvMissing()).isEqualTo("${ENV_MISSING}");
  }

  @Test
  void conditionalConfig() {
    assertThat(DW.getConfiguration().getOptionalConfig()).isNotNull();
  }

  @Test
  void shouldSupportNestedVariables() {
    assertThat(DW.getConfiguration().getExample()).startsWith("bar-").isEqualTo("bar-hello");
  }
}
