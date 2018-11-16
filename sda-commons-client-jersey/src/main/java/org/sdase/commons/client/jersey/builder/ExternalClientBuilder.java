package org.sdase.commons.client.jersey.builder;

import io.dropwizard.client.JerseyClientBuilder;

import javax.ws.rs.client.Client;

public class ExternalClientBuilder {

   private JerseyClientBuilder jerseyClientBuilder;

   public ExternalClientBuilder(JerseyClientBuilder jerseyClientBuilder) {
      this.jerseyClientBuilder = jerseyClientBuilder;
   }
   /**
    * Builds a generic client that can be used for Http requests.
    *
    * @param name the name of the client is used for metrics and thread names
    * @return the client instance
    */
   public Client buildGenericClient(String name) {
      return this.jerseyClientBuilder.build(name);
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
