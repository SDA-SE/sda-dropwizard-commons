package org.sdase.commons.client.jersey.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junitpioneer.jupiter.SetSystemProperty;

@DisabledIf(
    value = "org.sdase.commons.client.jersey.proxy.ClientProxyTestSetup#hostNotSet",
    disabledReason = "This test expects 'dummy.server.test' to point to '127.0.0.1' in /etc/hosts")
@SetSystemProperty(key = "http.proxyHost", value = "nowhere.example.com")
@SetSystemProperty(key = "http.nonProxyHosts", value = ClientProxyTestSetup.SERVER_DOMAIN)
class ProxyAndNoHostWithSystemPropertiesTest extends ClientProxyTestSetup {
  @Test
  void shouldCallWiremockServer() {
    assertThat(pingRequest()).isEqualTo("\"pong\"");
  }
}
