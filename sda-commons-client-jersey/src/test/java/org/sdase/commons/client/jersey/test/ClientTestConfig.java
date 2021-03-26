package org.sdase.commons.client.jersey.test;

import io.dropwizard.Configuration;
import javax.validation.Valid;
import org.sdase.commons.client.jersey.HttpClientConfiguration;
import org.sdase.commons.client.jersey.oidc.OidcConfiguration;

@SuppressWarnings("WeakerAccess")
public class ClientTestConfig extends Configuration {

  private String consumerToken;

  private String mockBaseUrl;

  private HttpClientConfiguration client = new HttpClientConfiguration();

  @Valid private OidcConfiguration oidc = new OidcConfiguration();

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

  public OidcConfiguration getOidc() {
    return oidc;
  }

  public ClientTestConfig setOidc(OidcConfiguration oidc) {
    this.oidc = oidc;
    return this;
  }
}
