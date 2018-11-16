package org.sdase.commons.client.jersey.test;

import io.dropwizard.Configuration;

public class ClientTestConfig extends Configuration {

   private String mockBaseUrl;

   @SuppressWarnings("WeakerAccess")
   public String getMockBaseUrl() {
      return mockBaseUrl;
   }

   @SuppressWarnings("unused")
   public ClientTestConfig setMockBaseUrl(String mockBaseUrl) {
      this.mockBaseUrl = mockBaseUrl;
      return this;
   }
}
