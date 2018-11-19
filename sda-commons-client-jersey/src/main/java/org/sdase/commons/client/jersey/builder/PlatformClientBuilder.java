package org.sdase.commons.client.jersey.builder;

import io.dropwizard.client.JerseyClientBuilder;
import org.sdase.commons.client.jersey.filter.AddRequestHeaderFilter;
import org.sdase.commons.client.jersey.filter.AuthHeaderClientFilter;
import org.sdase.commons.client.jersey.filter.TraceTokenClientFilter;
import org.sdase.commons.shared.tracing.ConsumerTracing;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class PlatformClientBuilder {

   private JerseyClientBuilder jerseyClientBuilder;

   private List<ClientRequestFilter> filters;

   private Supplier<Optional<String>> consumerTokenSupplier;

   public PlatformClientBuilder(JerseyClientBuilder jerseyClientBuilder, Supplier<Optional<String>> consumerTokenSupplier) {
      this.jerseyClientBuilder = jerseyClientBuilder;
      this.consumerTokenSupplier = consumerTokenSupplier;
      this.filters = new ArrayList<>();
      this.filters.add(new TraceTokenClientFilter());
   }

   /**
    * If authentication pass through is enabled, the JWT in the {@value javax.ws.rs.core.HttpHeaders#AUTHORIZATION}
    * header of an incoming request will be added to the outgoing request.
    *
    * @return this builder instance
    */
   public PlatformClientBuilder enableAuthenticationPassThrough() {
      this.filters.add(new AuthHeaderClientFilter());
      return this;
   }

   /**
    * If consumer token is enabled, the client will create a configured consumer token and add it as header to the
    * outgoing request.
    *
    * @return this builder instance
    */
   public PlatformClientBuilder enableConsumerToken() {
      if (consumerTokenSupplier == null) {
         throw new IllegalStateException("Trying to enableConsumerToken() without a supplier for the consumer token. "
               + "A Supplier for the consumer token has to be added in the JerseyClientBundle configuration: "
               + "JerseyClientBundle.builder().withConsumerTokenSupplier(Supplier<String>).build()");
      }
      filters.add(new AddRequestHeaderFilter(ConsumerTracing.TOKEN_HEADER, consumerTokenSupplier));
      return this;
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
