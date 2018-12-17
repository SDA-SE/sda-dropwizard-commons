package org.sdase.commons.server.cors.test;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A test application that provides endpoints for key sources to test loading over Http.
 */
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
