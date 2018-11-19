package org.sdase.commons.client.jersey;

import io.dropwizard.Bundle;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.client.jersey.builder.PlatformClientBuilder;
import org.sdase.commons.client.jersey.filter.ContainerRequestContextHolder;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * A bundle that provides Jersey clients with appropriate configuration for the SDA Platform.
 */
public class JerseyClientBundle implements Bundle {

   private ClientFactory clientFactory;

   private boolean initialized;

   private Supplier<Optional<String>> consumerTokenSupplier;

   public static InitialBuilder builder() {
      return new Builder();
   }

   private JerseyClientBundle(Supplier<Optional<String>> consumerTokenSupplier) {
      this.consumerTokenSupplier = consumerTokenSupplier;
   }

   @Override
   public void initialize(Bootstrap<?> bootstrap) {
      // no initialization needed here, we need the environment to initialize the client
   }

   @Override
   public void run(Environment environment) {
      JerseyClientBuilder clientBuilder = new JerseyClientBuilder(environment).using(new JerseyClientConfiguration());
      this.clientFactory = new ClientFactory(clientBuilder, consumerTokenSupplier);
      environment.jersey().register(ContainerRequestContextHolder.class);
      initialized = true;
   }

   public ClientFactory getClientFactory() {
      if (!initialized) {
         throw new IllegalStateException("Clients can be build in run(C, Environment), not in initialize(Bootstrap)");
      }
      return clientFactory;
   }


   //
   // Builder
   //

   public interface InitialBuilder extends FinalBuilder {
      /**
       * @param consumerTokenSupplier A supplier for the header value of the Http header
       *                              {@value org.sdase.commons.shared.tracing.ConsumerTracing#TOKEN_HEADER} that will
       *                              be send with each client request configured with
       *                              {@link PlatformClientBuilder#enableConsumerToken()}. If no such supplier is
       *                              configured, {@link PlatformClientBuilder#enableConsumerToken()} will fail.
       * @return a builder instance for further configuration
       */
      FinalBuilder withConsumerTokenSupplier(Supplier<Optional<String>> consumerTokenSupplier);
   }

   public interface FinalBuilder {
      JerseyClientBundle build();
   }

   public static class Builder implements InitialBuilder, FinalBuilder {

      private Supplier<Optional<String>> consumerTokenSupplier;

      @Override
      public FinalBuilder withConsumerTokenSupplier(Supplier<Optional<String>> consumerTokenSupplier) {
         this.consumerTokenSupplier = consumerTokenSupplier;
         return this;
      }

      @Override
      public JerseyClientBundle build() {
         return new JerseyClientBundle(consumerTokenSupplier);
      }
   }

}
