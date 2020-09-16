package org.sdase.commons.server.openapi.apps.file;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.openapitools.jackson.dataformat.hal.HALLink;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.net.URI;
import java.util.Collections;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import org.sdase.commons.server.dropwizard.ContextAwareEndpoint;
import org.sdase.commons.server.openapi.OpenApiBundle;
import org.sdase.commons.server.openapi.apps.test.HouseResource;

// don't specify an info since it would override the configuration in the openapi.yaml file
@OpenAPIDefinition()
@Path("")
public class FromFileTestApp extends Application<Configuration> implements ContextAwareEndpoint {
  private static final String HOUSE_PATH = "/house";

  @Context private UriInfo uriInfo;

  public static void main(String[] args) throws Exception {
    new FromFileTestApp().run(args);
  }

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    bootstrap.addBundle(
        OpenApiBundle.builder()
            .addResourcePackageClass(getClass())
            .withExistingOpenAPIFromClasspathResource("/custom-openapi.yaml")
            .build());
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    environment.jersey().register(this);
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
    URI self = uriInfo.getBaseUriBuilder().path(FromFileTestApp.class, "getHouse").build();
    return new HouseResource(
        Collections.emptyList(), Collections.emptyList(), new HALLink.Builder(self).build());
  }
}
