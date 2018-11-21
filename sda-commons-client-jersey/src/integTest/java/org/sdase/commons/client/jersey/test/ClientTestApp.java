package org.sdase.commons.client.jersey.test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.client.jersey.JerseyClientBundle;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationValueSupplierBundle;
import org.sdase.commons.server.trace.TraceTokenBundle;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/api")
public class ClientTestApp extends Application<ClientTestConfig> {

   private ConfigurationValueSupplierBundle<ClientTestConfig, String> consumerTokenSupplier =
         ConfigurationValueSupplierBundle.builder().withAccessor(ClientTestConfig::getConsumerName).build();
   private JerseyClientBundle jerseyClientBundle = JerseyClientBundle.builder()
         .withConsumerTokenSupplier(consumerTokenSupplier.supplier()).build();

   private MockApiClient mockApiClient;
   private MockApiClient externalMockApiClient;
   private MockApiClient authMockApiClient;


   public static void main(String[] args) throws Exception {
      new ClientTestApp().run(args);
   }

   @Override
   public void initialize(Bootstrap<ClientTestConfig> bootstrap) {
      bootstrap.getObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
      bootstrap.addBundle(TraceTokenBundle.builder().build());
      bootstrap.addBundle(jerseyClientBundle);
      bootstrap.addBundle(consumerTokenSupplier);
   }

   @Override
   public void run(ClientTestConfig configuration, Environment environment) {
      environment.jersey().register(this);

      mockApiClient = jerseyClientBundle.getClientFactory().platformClient()
            .api(MockApiClient.class)
            .atTarget(configuration.getMockBaseUrl());
      authMockApiClient = jerseyClientBundle.getClientFactory().platformClient()
            .enableAuthenticationPassThrough()
            .api(MockApiClient.class)
            .atTarget(configuration.getMockBaseUrl());
      externalMockApiClient = jerseyClientBundle.getClientFactory().externalClient()
            .api(MockApiClient.class)
            .atTarget(configuration.getMockBaseUrl());
   }

   public JerseyClientBundle getJerseyClientBundle() {
      return jerseyClientBundle;
   }

   @GET
   @Path("/cars")
   @Produces(MediaType.APPLICATION_JSON)
   public Response delegate() {
      return mockApiClient.requestCars();
   }

   @GET
   @Path("/carsExternal")
   @Produces(MediaType.APPLICATION_JSON)
   public Response delegateExternal() {
      return externalMockApiClient.requestCars();
   }

   @GET
   @Path("/carsAuth")
   @Produces(MediaType.APPLICATION_JSON)
   public Response delegateWithAuth() {
      return authMockApiClient.requestCars();
   }

}
