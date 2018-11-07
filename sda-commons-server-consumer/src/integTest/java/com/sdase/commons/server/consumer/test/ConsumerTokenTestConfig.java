package com.sdase.commons.server.consumer.test;

import com.sdase.commons.server.consumer.ConsumerTokenConfig;
import io.dropwizard.Configuration;

public class ConsumerTokenTestConfig extends Configuration {

   private ConsumerTokenConfig consumerToken = new ConsumerTokenConfig();

   public ConsumerTokenConfig getConsumerToken() {
      return consumerToken;
   }

   public void setConsumerToken(ConsumerTokenConfig consumerToken) {
      this.consumerToken = consumerToken;
   }
}
