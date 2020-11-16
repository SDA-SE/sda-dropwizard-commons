package org.sdase.commons.starter.test;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.Collections;
import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
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
            .withRequiredConsumerToken()
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
}
