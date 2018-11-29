package org.sdase.commons.client.jersey.builder;

import io.dropwizard.client.JerseyClientBuilder;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder that provides options that are common for all types of clients.
 *
 * @param <T> the type of the subclass
 */
abstract class AbstractBaseClientBuilder<T extends AbstractBaseClientBuilder> {

   private JerseyClientBuilder jerseyClientBuilder;

   private List<ClientRequestFilter> filters;

   AbstractBaseClientBuilder(JerseyClientBuilder jerseyClientBuilder) {
      this.jerseyClientBuilder = jerseyClientBuilder;
      this.filters = new ArrayList<>();
   }

   /**
    * Adds a request filter to the client.
    *
    * @param clientRequestFilter the filter to add
    *
    * @return this builder instance
    */
   public T addFilter(ClientRequestFilter clientRequestFilter) {
      this.filters.add(clientRequestFilter);
      //noinspection unchecked
      return (T) this;
   }

   /**
    * Builds a generic client that can be used for Http requests.
    *
    * @param name the name of the client is used for metrics and thread names
    * @return the client instance
    */
   public Client buildGenericClient(String name) {
      Client client = jerseyClientBuilder.build(name);
      filters.forEach(client::register);
      return client;
   }

   /**
    * Creates a client proxy implementation for accessing another service.
    *
    * @param apiInterface the interface that declares the API using JAX-RS annotations.
    * @param <A> the type of the api
    * @return a builder to define the root path of the API for the proxy that is build
    */
   public <A> ApiClientBuilder<A> api(Class<A> apiInterface) {
      return new ApiClientBuilder<>(apiInterface, buildGenericClient(apiInterface.getSimpleName()));
   }

}
