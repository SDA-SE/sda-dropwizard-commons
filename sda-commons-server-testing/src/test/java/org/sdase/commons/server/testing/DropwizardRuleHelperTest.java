package org.sdase.commons.server.testing;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.dropwizard.test.DropwizardApp;
import org.sdase.commons.server.dropwizard.test.DropwizardConfig;

public class DropwizardRuleHelperTest {

  @ClassRule
  public static DropwizardAppRule<DropwizardConfig> DW =
      DropwizardRuleHelper.dropwizardTestAppFrom(DropwizardApp.class)
          .withConfigFrom(DropwizardConfig::new)
          .withRandomPorts()
          .build();

  @Test
  public void localPortShouldBeGreaterThanZero() {
    assertThat(DW).isNotNull();
    assertThat(DW.getLocalPort()).isPositive();
  }
}
