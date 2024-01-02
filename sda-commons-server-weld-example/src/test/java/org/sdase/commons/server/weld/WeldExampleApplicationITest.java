package org.sdase.commons.server.weld;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.ext.cdi1x.internal.CdiComponentProvider;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.weld.beans.UsageBean;

@EnableAutoWeld
@AddPackages(WeldExampleApplication.class)
@AddExtensions(CdiComponentProvider.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WeldExampleApplicationITest {

  @RegisterExtension
  static DropwizardAppExtension<Configuration> APP =
      new DropwizardAppExtension<>(
          new DropwizardTestSupport<>(WeldExampleApplication.class, null, randomPorts()) {
            @Override
            public Application<Configuration> newApplication() {
              return CDI.current().select(WeldExampleApplication.class).get();
            }
          });

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
