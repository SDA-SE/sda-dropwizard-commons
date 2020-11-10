package org.sdase.commons.client.jersey.test;

import io.dropwizard.Configuration;
import org.sdase.commons.client.jersey.ApiHttpClientConfiguration;
import org.sdase.commons.client.jersey.HttpClientConfiguration;

@SuppressWarnings("WeakerAccess")
public class ClientTestConfig extends Configuration {

  private String consumerToken;

  private HttpClientConfiguration client = new HttpClientConfiguration();

  private ApiHttpClientConfiguration mockClient;

  public String getConsumerToken() {
    return consumerToken;
  }

  public ClientTestConfig setConsumerToken(String consumerToken) {
    this.consumerToken = consumerToken;
    return this;
  }

  public HttpClientConfiguration getClient() {
    return client;
  }

  public ClientTestConfig setClient(HttpClientConfiguration client) {
    this.client = client;
    return this;
  }

  public ApiHttpClientConfiguration getMockClient() {
    return mockClient;
  }

  public ClientTestConfig setMockClient(ApiHttpClientConfiguration mockClient) {
    this.mockClient = mockClient;
    return this;
  }
}
