package org.sdase.commons.server.swagger;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Api
@Path("")
public class SwaggerBundleTestApp extends Application<Configuration> {

   private static final String JOHN_DOE_PATH = "/jdoe";

   public static void main(String[] args) throws Exception {
      new SwaggerBundleTestApp().run(args);
   }

   @Override
   public void initialize(Bootstrap<Configuration> bootstrap) {
      bootstrap.addBundle(
            SwaggerBundle.builder()
                  .withTitle(getName())
                  .addResourcePackageClass(getClass())
                  .build());
   }

   @Override
   public void run(Configuration configuration, Environment environment) {
      environment.jersey().register(this);
   }

   @GET
   @Path(JOHN_DOE_PATH)
   @Produces(APPLICATION_JSON)
   @ApiOperation(value = "get", response = PersonResource.class)
   @ApiResponses(@ApiResponse(code = 200, message = "get", response = PersonResource.class))
   public PersonResource getJohnDoe() {
      return new PersonResource("John", "Doe");
   }

   @POST
   @Path(JOHN_DOE_PATH)
   @ApiOperation("post")
   @ApiResponses(@ApiResponse(code = 201, message = "post", responseHeaders = @ResponseHeader(name = "Location", description = "Location")))
   public Response createJohnDoe(@Context UriInfo uriInfo) {
      return Response.created(uriInfo.getBaseUriBuilder().path(JOHN_DOE_PATH).build()).build();
   }

   @DELETE
   @Path(JOHN_DOE_PATH)
   @ApiOperation("delete")
   @ApiResponses(@ApiResponse(code = 204, message = "deleted"))
   public void deleteJohnDoe() {
      // do nothing
   }
}
