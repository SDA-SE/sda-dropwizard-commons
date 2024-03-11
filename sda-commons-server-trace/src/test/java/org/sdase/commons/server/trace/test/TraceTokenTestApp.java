package org.sdase.commons.server.trace.test;

import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
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

  /**
   * @deprecated should use {@link org.sdase.commons.shared.tracing.TraceTokenContext} when {@link
   *     RequestTracing} is removed.
   */
  @GET
  @Path("/token")
  @Produces(MediaType.APPLICATION_JSON)
  @Deprecated(forRemoval = false)
  public Response getConsumerToken(@Context ContainerRequestContext requestContext) {
    return Response.ok(requestContext.getProperty(RequestTracing.TOKEN_ATTRIBUTE)).build();
  }
}
