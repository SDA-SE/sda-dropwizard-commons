package org.sdase.commons.server.cors.test;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Environment;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** A test application that provides endpoints for key sources to test loading over Http. */
@Path("/samples")
public abstract class CorsTestApp extends Application<CorsTestConfiguration> {

  @Override
  public void run(CorsTestConfiguration configuration, Environment environment) {
    environment.jersey().register(this);
  }

  @GET
  @Path("/empty")
  @Produces(MediaType.APPLICATION_JSON)
  public Response empty() {
    return Response.ok().build();
  }
}
