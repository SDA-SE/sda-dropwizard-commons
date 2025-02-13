package org.sdase.commons.server.opa.proxy;

import static org.assertj.core.api.Assertions.assertThatException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junitpioneer.jupiter.SetSystemProperty;

@DisabledIf(
    value = "org.sdase.commons.server.opa.proxy.OpaBundleProxyTestSetup#hostNotSet",
    disabledReason = "This test expects 'dummy.opa.test' to point to '127.0.0.1' in /etc/hosts")
@SetSystemProperty(key = "configureOpaTestProxy", value = "true")
class ProxyWithIndividualClientConfigTest extends OpaBundleProxyTestSetup {
  @Test
  void shouldFailToCallWiremockOpa() {
    assertThatException().isThrownBy(this::pingRequest);
  }
}
