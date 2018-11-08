package org.sdase.commons.server.consumer.test;

import org.sdase.commons.server.consumer.ConsumerTokenBundle;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.shared.tracing.ConsumerTracing;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class ConsumerTokenRequiredTestApp extends Application<ConsumerTokenTestConfig> {

   @Override
   public void initialize(Bootstrap<ConsumerTokenTestConfig> bootstrap) {
      bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
      bootstrap.addBundle(ConsumerTokenBundle.builder()
            .withRequiredConsumerToken()
            .build());
   }

   @Override
   public void run(ConsumerTokenTestConfig configuration, Environment environment) {
      environment.jersey().register(this);
   }

   @OPTIONS
   public Response getOptions() {
      return Response.ok().build();
   }

   @GET
   @Path("/name")
   @Produces(MediaType.APPLICATION_JSON)
   public Response getConsumerName(@Context ContainerRequestContext requestContext) {
      return Response.ok(requestContext.getProperty(ConsumerTracing.NAME_ATTRIBUTE)).build();
   }

   @GET
   @Path("/token")
   @Produces(MediaType.APPLICATION_JSON)
   public Response getConsumerToken(@Context ContainerRequestContext requestContext) {
      return Response.ok(requestContext.getProperty(ConsumerTracing.TOKEN_ATTRIBUTE)).build();
   }

}
