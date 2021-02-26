package org.sdase.commons.server.openapi.apps.missingoperation;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.sdase.commons.server.openapi.OpenApiBundle;

@Path("")
@OpenAPIDefinition(info = @Info(title = "A test app", description = "Test", version = "1"))
public class MissingOperationTestApp extends Application<Configuration> {

  /** Note that this method does not use {@link io.swagger.v3.oas.annotations.Operation}. */
  @GET
  @Path("/foo")
  @Produces(APPLICATION_JSON)
  @ApiResponse(
      responseCode = "200",
      description = "get",
      content =
          @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = String.class)))
  public String getFoo() {
    return "foo";
  }

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    bootstrap.addBundle(OpenApiBundle.builder().addResourcePackageClass(getClass()).build());
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    environment.jersey().register(this);
  }
}
