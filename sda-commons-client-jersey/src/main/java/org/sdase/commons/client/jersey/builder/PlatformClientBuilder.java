package org.sdase.commons.client.jersey.builder;

import io.dropwizard.client.JerseyClientBuilder;
import org.apache.commons.lang3.NotImplementedException;
import org.sdase.commons.client.jersey.filter.AuthHeaderClientFilter;
import org.sdase.commons.client.jersey.filter.TraceTokenClientFilter;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import java.util.ArrayList;
import java.util.List;

public class PlatformClientBuilder {

   private JerseyClientBuilder jerseyClientBuilder;

   private List<Class<? extends ClientRequestFilter>> filters;


   public PlatformClientBuilder(JerseyClientBuilder jerseyClientBuilder) {
      this.jerseyClientBuilder = jerseyClientBuilder;
      this.filters = new ArrayList<>();
      this.filters.add(TraceTokenClientFilter.class);
   }

   /**
    * If authentication pass through is enabled, the JWT in the {@value javax.ws.rs.core.HttpHeaders#AUTHORIZATION}
    * header of an incoming request will be added to the outgoing request.
    *
    * @return this builder instance
    */
   public PlatformClientBuilder enableAuthenticationPassThrough() {
      this.filters.add(AuthHeaderClientFilter.class);
      return this;
   }

   /**
    * If consumer token is enabled, the client will create a configured consumer token and add it as header to the
    * outgoing request.
    *
    * @return this builder instance
    */
   public PlatformClientBuilder enableConsumerToken() {
      // TODO implement me
      throw new NotImplementedException("Adding a consumer token to the outgoing request is not implemented yet.");
   }

   /**
    * Builds a generic client that will send a {@code Trace-Token} to the server  and may add other headers as
    * configured in this builder.
    *
    * @param name the name of the client is used for metrics and thread names
    * @return the client instance
    */
   public Client buildGenericClient(String name) {
      Client client = this.jerseyClientBuilder.build(name);
      filters.forEach(client::register);
      return client;
   }

   /**
    * Creates a client proxy implementation for accessing another service within the SDA platform. The client will send
    * a {@code Trace-Token} to the API server and may add other headers as configured in this builder.
    *
    * @param apiInterface the interface that declares the API using JAX-RS annotations.
    * @param <A> the type of the api
    * @return a builder to define the root path of the API for the proxy that is build
    */
   public <A> ApiClientBuilder<A> api(Class<A> apiInterface) {
      return new ApiClientBuilder<>(apiInterface, buildGenericClient(apiInterface.getSimpleName()));
   }

}
