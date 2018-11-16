package org.sdase.commons.client.jersey;

import io.dropwizard.Bundle;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.client.jersey.filter.ContainerRequestContextHolder;

/**
 * A bundle that provides Jersey clients with appropriate configuration for the SDA Platform.
 */
public class JerseyClientBundle implements Bundle {

   private ClientFactory clientFactory;

   private boolean initialized;

   @Override
   public void initialize(Bootstrap<?> bootstrap) {
      // no initialization needed here, we need the environment to initialize the client
   }

   @Override
   public void run(Environment environment) {
      JerseyClientBuilder clientBuilder = new JerseyClientBuilder(environment).using(new JerseyClientConfiguration());
      this.clientFactory = new ClientFactory(clientBuilder);
      environment.jersey().register(ContainerRequestContextHolder.class);
      initialized = true;
   }

   public ClientFactory getClientFactory() {
      if (!initialized) {
         throw new IllegalStateException("Clients can be build in run(C, Environment), not in initialize(Bootstrap)");
      }
      return clientFactory;
   }

}
