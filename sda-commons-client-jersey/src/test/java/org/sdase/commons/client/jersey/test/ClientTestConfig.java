package org.sdase.commons.client.jersey.test;

import io.dropwizard.Configuration;

@SuppressWarnings("WeakerAccess")
public class ClientTestConfig extends Configuration {

   private String consumerToken;

   private String mockBaseUrl;

   public String getConsumerToken() {
      return consumerToken;
   }

   public ClientTestConfig setConsumerToken(String consumerToken) {
      this.consumerToken = consumerToken;
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
