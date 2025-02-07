package org.sdase.commons.server.auth.config;

import io.dropwizard.client.JerseyClientConfiguration;

public class KeycloakLoaderClientConfig extends JerseyClientConfiguration {
  public KeycloakLoaderClientConfig() {
    // since Apache HTTP5 Client 7.4, proxied connections request an upgrade for TLS/1.2 by default
    // this upgrade request breaks outgoing istio proxies, so we disable connection upgrades
    // https://issues.apache.org/jira/browse/HTTPCLIENT-751
    // https://github.com/istio/istio/issues/53239
    // https://github.com/envoyproxy/envoy/issues/36305
    //
    // also see HttpClientConfiguration in client-jersey
    setProtocolUpgradeEnabled(false);
  }
}
