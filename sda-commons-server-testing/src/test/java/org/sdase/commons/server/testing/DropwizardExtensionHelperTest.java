package org.sdase.commons.server.testing;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sdase.commons.server.dropwizard.test.DropwizardApp;
import org.sdase.commons.server.dropwizard.test.DropwizardConfig;

@ExtendWith(DropwizardExtensionsSupport.class)
class DropwizardExtensionHelperTest {

  private static DropwizardAppExtension<DropwizardConfig> DW =
      DropwizardExtensionHelper.dropwizardTestAppFrom(DropwizardApp.class)
          .withConfigFrom(DropwizardConfig::new)
          .withRandomPorts()
          .build();

  @Test
  void localPortShouldBeGreaterThanZero() {
    assertThat(DW).isNotNull();
    assertThat(DW.getLocalPort()).isPositive();
  }
}
