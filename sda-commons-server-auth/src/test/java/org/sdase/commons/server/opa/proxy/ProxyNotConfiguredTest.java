package org.sdase.commons.server.opa.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

@DisabledIf(
    value = "org.sdase.commons.server.opa.proxy.OpaBundleProxyTestSetup#hostNotSet",
    disabledReason = "This test expects 'dummy.opa.test' to point to '127.0.0.1' in /etc/hosts")
class ProxyNotConfiguredTest extends OpaBundleProxyTestSetup {
  @Test
  void shouldCallWiremockOpa() {
    var actual = pingRequest();
    assertThat(actual).isEqualTo("pong");
  }
}
