package org.sdase.commons.server.dropwizard.bundles;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.assertj.core.api.Assertions;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.dropwizard.test.DropwizardApp;
import org.sdase.commons.server.dropwizard.test.DropwizardConfig;
import org.sdase.commons.server.testing.SystemPropertyRule;

public class ConfigurationSubstitutionBundleWithoutOptionalConfigTest {

  @ClassRule
  public static final SystemPropertyRule ENV =
      new SystemPropertyRule().setProperty("OPTIONAL_CONFIG", "null");

  @ClassRule
  public static final DropwizardAppRule<DropwizardConfig> DW =
      new DropwizardAppRule<>(
          DropwizardApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

  @Test
  public void optionalConfigShouldNotExist() {
    Assertions.assertThat(DW.getConfiguration().getOptionalConfig()).isNull();
  }
}
