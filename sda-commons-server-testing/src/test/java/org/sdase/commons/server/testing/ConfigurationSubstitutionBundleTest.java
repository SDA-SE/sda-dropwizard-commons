package org.sdase.commons.server.testing;

import org.sdase.commons.server.testing.test.DropwizardApp;
import org.sdase.commons.server.testing.test.DropwizardConfig;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.assertj.core.api.Assertions;
import org.junit.ClassRule;
import org.junit.Test;

public class ConfigurationSubstitutionBundleTest {

   @ClassRule
   public static final EnvironmentRule ENV = new EnvironmentRule().setEnv("envReplaced", "valueFromEnv");

   @ClassRule
   public static final DropwizardAppRule<DropwizardConfig> DW = new DropwizardAppRule<>(
         DropwizardApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

   @Test
   public void shouldReplaceWithoutDefaultDefined() {
      Assertions.assertThat(DW.getConfiguration().getEnvReplaced()).isEqualTo("valueFromEnv");
   }

   @Test
   public void shouldReplaceWithDefaultDefined() {
      Assertions.assertThat(DW.getConfiguration().getEnvWithDefaultReplaced()).isEqualTo("valueFromEnv");
   }

   @Test
   public void shouldUseDefaultIfEnvNotDefined() {
      Assertions.assertThat(DW.getConfiguration().getEnvDefault()).isEqualTo("DEFAULT");
   }

   @Test
   public void shouldKeepPlaceholderIfNoEnvAndNoDefault() {
      Assertions.assertThat(DW.getConfiguration().getEnvMissing()).isEqualTo("${envMissing}");
   }
}