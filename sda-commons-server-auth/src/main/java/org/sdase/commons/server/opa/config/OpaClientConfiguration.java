package org.sdase.commons.server.opa.config;

import io.dropwizard.client.JerseyClientConfiguration;

/** A customized {@link JerseyClientConfiguration} that disables gzip by default. */
public class OpaClientConfiguration extends JerseyClientConfiguration {
  public OpaClientConfiguration() {
    setGzipEnabled(false);
    setGzipEnabledForRequests(false);
  }
}
