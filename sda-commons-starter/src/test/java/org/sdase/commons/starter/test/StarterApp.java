package org.sdase.commons.starter.test;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;
import org.sdase.commons.server.dropwizard.metadata.DetachedMetadataContext;
import org.sdase.commons.server.dropwizard.metadata.MetadataContext;
import org.sdase.commons.starter.SdaPlatformBundle;
import org.sdase.commons.starter.SdaPlatformConfiguration;

@Produces(MediaType.APPLICATION_JSON)
@Path("")
@OpenAPIDefinition(
    info =
        @Info(
            title = "Starter",
            description = "A test application",
            version = "1.1.1",
            contact =
                @Contact(name = "John Doe", email = "info@example.com", url = "j.doe@example.com"),
            license = @License(name = "Sample License")))
public class StarterApp extends Application<SdaPlatformConfiguration> {

  public static void main(String[] args) throws Exception {
    new StarterApp().run(args);
  }

  @Override
  public void initialize(Bootstrap<SdaPlatformConfiguration> bootstrap) {
    bootstrap.addBundle(
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .addOpenApiResourcePackageClass(this.getClass())
            .build());
  }

  @Override
  public void run(SdaPlatformConfiguration configuration, Environment environment) {
    environment.jersey().register(this);
  }

  @GET
  @PermitAll
  @Path("ping")
  @Operation(
      description = "Answers the ping request with a pong.",
      summary = "The ping endpoint is usually used to check if the application is alive.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "Successful ping")})
  public Object getPing() {
    return Collections.singletonMap("ping", "pong");
  }

  @GET
  @PermitAll
  @Path("metadataContext")
  public DetachedMetadataContext getContext(@HeaderParam("tenant-id") List<String> header) {
    return MetadataContext.detachedCurrent();
  }
}
