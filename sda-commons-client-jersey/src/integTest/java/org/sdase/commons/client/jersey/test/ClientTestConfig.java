package org.sdase.commons.client.jersey.test;

import io.dropwizard.Configuration;

@SuppressWarnings("WeakerAccess")
public class ClientTestConfig extends Configuration {

   private String consumerName;

   private String mockBaseUrl;

   public String getConsumerName() {
      return consumerName;
   }

   public ClientTestConfig setConsumerName(String consumerName) {
      this.consumerName = consumerName;
      return this;
   }

   public String getMockBaseUrl() {
      return mockBaseUrl;
   }

   public ClientTestConfig setMockBaseUrl(String mockBaseUrl) {
      this.mockBaseUrl = mockBaseUrl;
      return this;
   }
}
