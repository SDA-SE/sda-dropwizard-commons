package org.sdase.commons.server.weld;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.weld.beans.ExampleProducer;
import org.sdase.commons.server.weld.beans.UsageBean;

@EnableAutoWeld
@AddBeanClasses({ExampleProducer.class, UsageBean.class})
class WeldJunit5AutoExtensionTest {

  @Test
  void shouldBeInjectedCorrectly(WeldExampleApplication application) {
    assertThat(application).isNotNull();
    assertThat(application.getUsageBean()).isNotNull();
  }
}
