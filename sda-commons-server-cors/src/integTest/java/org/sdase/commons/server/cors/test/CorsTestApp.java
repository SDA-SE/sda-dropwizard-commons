package org.sdase.commons.server.cors.test;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.cors.CorsBundle;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A test application that provides endpoints for key sources to test loading over Http.
 */
@Path("/samples")
public class CorsTestApp extends Application<CorsTestConfiguration> {

   public static void main(String[] args) throws Exception {
      new CorsTestApp().run(args);
   }

   @Override
   public void initialize(Bootstrap<CorsTestConfiguration> bootstrap) {
      bootstrap.addBundle(CorsBundle.builder().withCorsConfigProvider(CorsTestConfiguration::getCors).build());
   }

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
