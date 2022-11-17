package org.sdase.commons.server.weld;

import static io.dropwizard.testing.ConfigOverride.randomPorts;

import io.dropwizard.Configuration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.weld.testing.WeldAppExtension;

class WeldExampleApplicationITest {

  @RegisterExtension
  static final WeldAppExtension<Configuration> APP =
      new WeldAppExtension<>(WeldExampleApplication.class, null, randomPorts());

  @Test
  void shouldBeInjectedCorrectly() {
    WeldExampleApplication app = APP.getApplication();
    Assertions.assertThat(app.getUsageBean()).isNotNull();
  }
}
