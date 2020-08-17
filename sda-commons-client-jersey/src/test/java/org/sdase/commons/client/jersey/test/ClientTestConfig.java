package org.sdase.commons.client.jersey.test;

import io.dropwizard.Configuration;
import org.sdase.commons.client.jersey.HttpClientConfiguration;

@SuppressWarnings("WeakerAccess")
public class ClientTestConfig extends Configuration {

  private String consumerToken;

  private String mockBaseUrl;

  private HttpClientConfiguration client = new HttpClientConfiguration();

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

  public HttpClientConfiguration getClient() {
    return client;
  }

  public ClientTestConfig setClient(HttpClientConfiguration client) {
    this.client = client;
    return this;
  }
}
