package org.sdase.commons.server.dropwizard.bundles.test;

import io.dropwizard.core.Configuration;
import org.sdase.commons.server.dropwizard.bundles.ConsumerTokenConfig;

public class ConsumerTokenTestConfig extends Configuration {

  private ConsumerTokenConfig consumerToken = new ConsumerTokenConfig();

  public ConsumerTokenConfig getConsumerToken() {
    return consumerToken;
  }

  public void setConsumerToken(ConsumerTokenConfig consumerToken) {
    this.consumerToken = consumerToken;
  }
}
