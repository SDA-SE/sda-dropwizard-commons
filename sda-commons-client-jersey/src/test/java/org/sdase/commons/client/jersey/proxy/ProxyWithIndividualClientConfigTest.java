package org.sdase.commons.client.jersey.proxy;

import static org.assertj.core.api.Assertions.assertThatException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junitpioneer.jupiter.SetSystemProperty;

@DisabledIf(
    value = "org.sdase.commons.client.jersey.proxy.ClientProxyTestSetup#hostNotSet",
    disabledReason = "This test expects 'dummy.server.test' to point to '127.0.0.1' in /etc/hosts")
@SetSystemProperty(key = "configureClientTestProxy", value = "true")
class ProxyWithIndividualClientConfigTest extends ClientProxyTestSetup {
  @Test
  void shouldFailToCallWiremockServer() {
    assertThatException().isThrownBy(this::pingRequest);
  }
}
