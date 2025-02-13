package org.sdase.commons.client.jersey.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

@DisabledIf(
    value = "org.sdase.commons.client.jersey.proxy.ClientProxyTestSetup#hostNotSet",
    disabledReason = "This test expects 'dummy.server.test' to point to '127.0.0.1' in /etc/hosts")
class ProxyNotConfiguredTest extends ClientProxyTestSetup {
  @Test
  void shouldCallWiremockServer() {
    var actual = pingRequest();
    assertThat(actual).isEqualTo("\"pong\"");
  }
}
