package org.sdase.commons.server.consumer.test;

import io.dropwizard.Configuration;
import org.sdase.commons.server.consumer.ConsumerTokenConfig;

public class ConsumerTokenTestConfig extends Configuration {

  private ConsumerTokenConfig consumerToken = new ConsumerTokenConfig();

  public ConsumerTokenConfig getConsumerToken() {
    return consumerToken;
  }

  public void setConsumerToken(ConsumerTokenConfig consumerToken) {
    this.consumerToken = consumerToken;
  }
}
