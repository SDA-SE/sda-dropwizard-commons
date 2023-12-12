package org.sdase.commons.server.trace.test;

import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.trace.TraceTokenBundle;
import org.sdase.commons.shared.tracing.RequestTracing;

@Path("/")
public class TraceTokenTestApp extends Application<Configuration> {

  @Override
  public void initialize(Bootstrap bootstrap) {
    bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
    bootstrap.addBundle(TraceTokenBundle.builder().build());
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    environment.jersey().register(this);
  }

  @OPTIONS
  public Response getOptions() {
    return Response.ok().build();
  }

  @GET
  @Path("/token")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getConsumerToken(@Context ContainerRequestContext requestContext) {
    return Response.ok(requestContext.getProperty(RequestTracing.TOKEN_ATTRIBUTE)).build();
  }
}
