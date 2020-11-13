package org.sdase.commons.client.jersey.builder;

import static org.sdase.commons.client.jersey.proxy.ApiClientInvocationHandler.createProxy;

import javax.ws.rs.client.Client;
import org.glassfish.jersey.client.proxy.WebResourceFactory;

/**
 * Builder to create clients from JAX-RS annotated interfaces.
 *
 * @param <A> the client interface
 * @deprecated a {@linkplain org.sdase.commons.client.jersey.ClientFactory#apiClient(Class) new API}
 *     with a dedicated {@linkplain org.sdase.commons.client.jersey.ApiHttpClientConfiguration
 *     configuration class} is available to configure API clients based on interfaces. The new
 *     {@link FluentApiClientBuilder} API provides the same features as this builder but uses all
 *     configurable options in a the dedicated {@link
 *     org.sdase.commons.client.jersey.ApiHttpClientConfiguration} and therefore enforces to have
 *     all configuration options (like proxy settings) available for operators who need them.
 */
@Deprecated
public class ApiClientBuilder<A> {

  private Class<A> apiClass;

  private Client client;

  ApiClientBuilder(Class<A> apiClass, Client client) {
    this.apiClass = apiClass;
    this.client = client;
  }

  /**
   * Creates the client proxy that can be used to access the api.
   *
   * @param baseUri the base uri of the API, e.g. http://myservice.sda-se.org/api
   * @return the client proxy implementing the client interface
   */
  public A atTarget(String baseUri) {
    return createProxy(apiClass, WebResourceFactory.newResource(apiClass, client.target(baseUri)));
  }
}
