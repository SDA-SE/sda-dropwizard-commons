package org.sdase.commons.server.weld;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Configuration;
import javax.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.weld.beans.UsageBean;
import org.sdase.commons.server.weld.testing.WeldAppExtension;

class WeldExampleApplicationITest {

  @RegisterExtension
  static final WeldAppExtension<Configuration> APP =
      new WeldAppExtension<>(WeldExampleApplication.class, null, randomPorts());

  @Test
  void shouldBeInjectedCorrectly() {
    WeldExampleApplication app = APP.getApplication();
    assertThat(app.getUsageBean()).isNotNull().isInstanceOf(UsageBean.class);
  }

  @Test
  void shouldGetFromRest() {
    String result =
        APP.client()
            .target(String.format("http://localhost:%d/someString", APP.getLocalPort()))
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get(String.class);
    assertThat(result).isEqualTo("some string");
  }
}
