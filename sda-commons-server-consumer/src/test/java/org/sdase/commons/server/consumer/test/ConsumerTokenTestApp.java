package org.sdase.commons.server.consumer.test;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.sdase.commons.server.consumer.ConsumerTokenBundle;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.jackson.JacksonConfigurationBundle;
import org.sdase.commons.shared.tracing.ConsumerTracing;

@Path("/")
public class ConsumerTokenTestApp extends Application<ConsumerTokenTestConfig> {

  @Override
  public void initialize(Bootstrap<ConsumerTokenTestConfig> bootstrap) {
    bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
    bootstrap.addBundle(JacksonConfigurationBundle.builder().build());
    bootstrap.addBundle(
        ConsumerTokenBundle.builder()
            .withConfigProvider(ConsumerTokenTestConfig::getConsumerToken)
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
  @Path("/swagger.json")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSwagger(@Context ContainerRequestContext requestContext) {
    return Response.ok(requestContext.getProperty(ConsumerTracing.NAME_ATTRIBUTE)).build();
  }

  @GET
  @Path("/openapi.json")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getOpenApi(@Context ContainerRequestContext requestContext) {
    return Response.ok(requestContext.getProperty(ConsumerTracing.NAME_ATTRIBUTE)).build();
  }

  @GET
  @Path("/token")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getConsumerToken(@Context ContainerRequestContext requestContext) {
    return Response.ok(requestContext.getProperty(ConsumerTracing.TOKEN_ATTRIBUTE)).build();
  }
}
