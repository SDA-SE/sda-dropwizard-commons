package org.sdase.commons.server.weld;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.jboss.weld.junit5.auto.SetBeanDiscoveryMode;
import org.junit.jupiter.api.Test;

@EnableAutoWeld
@AddPackages(WeldExampleApplication.class)
@SetBeanDiscoveryMode(BeanDiscoveryMode.ALL)
class WeldJunit5AutoExtensionTest {

  @Test
  void shouldBeInjectedCorrectly(WeldExampleApplication application) {
    assertThat(application).isNotNull();
    assertThat(application.getUsageBean()).isNotNull();
    assertThat(application.getSomeString()).isEqualTo("some string");
  }
}
