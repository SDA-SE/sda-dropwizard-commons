package org.sdase.commons.server.openapi.apps.test;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.Collections.emptyList;

import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.openapitools.jackson.dataformat.hal.HALLink;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import org.sdase.commons.server.dropwizard.ContextAwareEndpoint;
import org.sdase.commons.server.openapi.OpenApiBundle;

@Path("")
@OpenAPIDefinition(info = @Info(title = "A test app", description = "Test", version = "1"))
public class OpenApiBundleTestApp extends Application<Configuration>
    implements ContextAwareEndpoint {

  private static final String JOHN_DOE_PATH = "/jdoe";
  private static final String HOUSE_PATH = "/house";

  @Context private UriInfo uriInfo;

  public static void main(String[] args) throws Exception {
    new OpenApiBundleTestApp().run(args);
  }

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    bootstrap.addBundle(OpenApiBundle.builder().addResourcePackageClass(getClass()).build());
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    environment.jersey().register(this);
  }

  @GET
  @Path(JOHN_DOE_PATH)
  @Produces(APPLICATION_JSON)
  @Operation(summary = "get")
  @ApiResponse(
      responseCode = "200",
      description = "get",
      content =
          @Content(
              mediaType = APPLICATION_JSON,
              schema = @Schema(implementation = PartnerResource.class)))
  public PartnerResource getJohnDoe() {
    URI self = uriInfo.getBaseUriBuilder().path(OpenApiBundleTestApp.class, "getJohnDoe").build();
    return new NaturalPersonResource("John", "Doe", emptyList(), new HALLink.Builder(self).build());
  }

  @POST
  @Path(JOHN_DOE_PATH)
  @Operation(summary = "post")
  @ApiResponse(
      responseCode = "201",
      description = "post",
      headers = {@Header(name = "Location", description = "Location")})
  public Response createJohnDoe(@Context UriInfo uriInfo) {
    return Response.created(uriInfo.getBaseUriBuilder().path(JOHN_DOE_PATH).build()).build();
  }

  @DELETE
  @Path(JOHN_DOE_PATH)
  @Operation(description = "delete")
  @ApiResponse(responseCode = "204", description = "deleted")
  public void deleteJohnDoe() {
    // do nothing
  }

  @GET
  @Path(HOUSE_PATH)
  @Produces(APPLICATION_JSON)
  @Operation(description = "get")
  @ApiResponse(
      responseCode = "200",
      description = "get",
      content =
          @Content(
              mediaType = APPLICATION_JSON,
              schema = @Schema(implementation = HouseResource.class)))
  public HouseResource getHouse() {
    URI self = uriInfo.getBaseUriBuilder().path(OpenApiBundleTestApp.class, "getHouse").build();
    return new HouseResource(
        Collections.emptyList(), Collections.emptyList(), new HALLink.Builder(self).build());
  }

  @GET
  @Path("/houses")
  @Produces(APPLICATION_JSON)
  @Operation()
  @ApiResponse(
      responseCode = "200",
      description = "get",
      content =
          @Content(
              mediaType = APPLICATION_JSON,
              schema = @Schema(implementation = HouseSearchResource.class)))
  public HouseSearchResource searchHouse() {
    return new HouseSearchResource(Collections.emptyList(), Collections.emptyList(), 0);
  }

  @GET
  @Path("/partners")
  @Produces(APPLICATION_JSON)
  @Operation()
  @ApiResponse(
      responseCode = "200",
      description = "get",
      content =
          @Content(
              mediaType = APPLICATION_JSON,
              schema = @Schema(implementation = PartnerSearchResultResource.class)))
  public PartnerSearchResultResource searchPartners() {
    return new PartnerSearchResultResource()
        .setPartners(Arrays.asList("one", "two"))
        .setTimestamp(ZonedDateTime.now())
        .setTotalResults(100);
  }
}
