package org.sdase.commons.client.jersey.builder;

import io.dropwizard.client.JerseyClientBuilder;
import org.apache.commons.lang3.StringUtils;
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

   public PlatformClientBuilder(JerseyClientBuilder jerseyClientBuilder, String consumerToken) {
      this.jerseyClientBuilder = jerseyClientBuilder;
      this.consumerTokenSupplier = () -> Optional.ofNullable(StringUtils.trimToNull(consumerToken));
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
      return addFilter(new AuthHeaderClientFilter());
   }

   /**
    * If consumer token is enabled, the client will create a configured consumer token and add it as header to the
    * outgoing request.
    *
    * @return this builder instance
    */
   public PlatformClientBuilder enableConsumerToken() {
      return addFilter(new AddRequestHeaderFilter() {
         @Override
         public String getHeaderName() {
            return ConsumerTracing.TOKEN_HEADER;
         }
         @Override
         public Optional<String> getHeaderValue() {
            return consumerTokenSupplier.get();
         }
      });
   }

   /**
    * Adds a request filter to the client.
    *
    * @param clientRequestFilter the filter to add
    *
    * @return this builder instance
    */
   public PlatformClientBuilder addFilter(ClientRequestFilter clientRequestFilter) {
      this.filters.add(clientRequestFilter);
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
