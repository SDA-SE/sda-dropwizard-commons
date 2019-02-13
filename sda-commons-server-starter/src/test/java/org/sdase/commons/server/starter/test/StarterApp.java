package org.sdase.commons.server.starter.test;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import javax.annotation.security.PermitAll;
import org.sdase.commons.server.starter.SdaPlatformBundle;
import org.sdase.commons.server.starter.SdaPlatformConfiguration;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import java.util.Collections;

@Api(produces = MediaType.APPLICATION_JSON)
@Path("")
public class StarterApp extends Application<SdaPlatformConfiguration> {

   public static void main(String[] args) throws Exception {
      new StarterApp().run(args);
   }

   @Override
   public void initialize(Bootstrap<SdaPlatformConfiguration> bootstrap) {
      bootstrap.addBundle(SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            .withSwaggerInfoTitle("Starter")
            .withSwaggerInfoVersion("1.1.1")
            .withSwaggerInfoDescription("A test application")
            .withSwaggerInfoLicense("Sample License")
            .withSwaggerInfoContact("John Doe", "j.doe@example.com")
            .addSwaggerResourcePackageClass(this.getClass())
            .build());
   }

   @Override
   public void run(SdaPlatformConfiguration configuration, Environment environment) {
      environment.jersey().register(this);
   }

   @GET
   @PermitAll
   @Path("ping")
   @ApiOperation(
         value = "Answers the ping request with a pong.",
         notes = "The ping endpoint is usually used to check if the application is alive."
   )
   @ApiResponses({
         @ApiResponse(
               code = 200,
               message = "Successful ping",
               examples = @Example(@ExampleProperty("{\"ping\":\"pong\"}")))
   })
   public Object getPing() {
      return Collections.singletonMap("ping", "pong");
   }
}
