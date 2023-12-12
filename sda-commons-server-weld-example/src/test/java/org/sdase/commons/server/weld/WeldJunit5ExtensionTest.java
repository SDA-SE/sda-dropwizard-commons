package org.sdase.commons.server.weld;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.weld.beans.ExampleProducer;
import org.sdase.commons.server.weld.beans.SimpleBean;
import org.sdase.commons.server.weld.beans.UsageBean;

@EnableWeld
class WeldJunit5ExtensionTest {

  @WeldSetup
  WeldInitiator weld =
      WeldInitiator.of(
          WeldExampleApplication.class, SimpleBean.class, UsageBean.class, ExampleProducer.class);

  @Test
  void shouldBeInjectedCorrectly(WeldExampleApplication application) {
    assertThat(application).isNotNull();
    assertThat(application.getUsageBean()).isNotNull();
  }
}
