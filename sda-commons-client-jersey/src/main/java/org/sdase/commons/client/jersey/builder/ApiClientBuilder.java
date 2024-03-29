package org.sdase.commons.client.jersey.builder;

import static org.sdase.commons.client.jersey.proxy.ApiClientInvocationHandler.createProxy;

import jakarta.ws.rs.client.Client;
import org.glassfish.jersey.client.proxy.WebResourceFactory;

/**
 * Builder to create clients from JAX-RS annotated interfaces.
 *
 * @param <A> the client interface
 */
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
