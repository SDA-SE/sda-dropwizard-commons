package org.sdase.commons.server.swagger;

import static java.util.Collections.emptyList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.openapitools.jackson.dataformat.hal.HALLink;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
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
  private static final String HOUSE_PATH = "/house";

  private static AtomicInteger counter = new AtomicInteger(0);
  private int instanceNumber;

  @Context private UriInfo uriInfo;

  public static void main(String[] args) throws Exception {
    new SwaggerBundleTestApp().run(args);
  }

  public String getTitle() {
    return getName() + instanceNumber;
  }

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    instanceNumber = counter.incrementAndGet();
    bootstrap.addBundle(
        SwaggerBundle.builder().withTitle(getTitle()).addResourcePackageClass(getClass()).build());
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    environment.jersey().register(this);
  }

  @GET
  @Path(JOHN_DOE_PATH)
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "get", response = PartnerResource.class)
  @ApiResponses(@ApiResponse(code = 200, message = "get", response = PartnerResource.class))
  public PartnerResource getJohnDoe() {
    URI self = uriInfo.getBaseUriBuilder().path(SwaggerBundleTestApp.class, "getJohnDoe").build();
    return new NaturalPersonResource("John", "Doe", emptyList(), new HALLink.Builder(self).build());
  }

  @POST
  @Path(JOHN_DOE_PATH)
  @ApiOperation("post")
  @ApiResponses(
      @ApiResponse(
          code = 201,
          message = "post",
          responseHeaders = @ResponseHeader(name = "Location", description = "Location")))
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

  @GET
  @Path(HOUSE_PATH)
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "get", response = HouseResource.class)
  @ApiResponses(@ApiResponse(code = 200, message = "get", response = HouseResource.class))
  public HouseResource getHouse() {
    URI self = uriInfo.getBaseUriBuilder().path(SwaggerBundleTestApp.class, "getHouse").build();
    return new HouseResource(
        Collections.emptyList(), Collections.emptyList(), new HALLink.Builder(self).build());
  }
}
