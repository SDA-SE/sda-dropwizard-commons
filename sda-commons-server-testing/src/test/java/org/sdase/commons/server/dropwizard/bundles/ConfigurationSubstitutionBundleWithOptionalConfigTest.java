package org.sdase.commons.server.dropwizard.bundles;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.dropwizard.test.DropwizardApp;
import org.sdase.commons.server.dropwizard.test.DropwizardConfig;
import org.sdase.commons.server.testing.SystemPropertyClassExtension;

class ConfigurationSubstitutionBundleWithOptionalConfigTest {

  @RegisterExtension
  @Order(0)
  static final SystemPropertyClassExtension SYSTEM_PROPERTY_RULE =
      new SystemPropertyClassExtension().setProperty("PROPERTY_ONE", "valueFromEnv");

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<DropwizardConfig> DW =
      new DropwizardAppExtension<>(
          DropwizardApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

  @Test
  void shouldReplaceInNestedProperties() {
    Assertions.assertThat(DW.getConfiguration().getOptionalConfig().getProperty1())
        .isEqualTo("valueFromEnv");
  }
}
