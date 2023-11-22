package org.sdase.commons.server.jackson.test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.sdase.commons.server.dropwizard.ContextAwareEndpoint;
import org.sdase.commons.server.jackson.JacksonConfigurationBundle;

@Path("")
public class JacksonConfigurationInvalidTypeTestApp extends Application<Configuration>
    implements ContextAwareEndpoint {

  public static void main(String[] args) throws Exception {
    new JacksonConfigurationInvalidTypeTestApp().run(args);
  }

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    bootstrap.addBundle(
        JacksonConfigurationBundle.builder()
            .withCustomization(om -> om.enable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE))
            .build());
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    environment.jersey().register(this);
  }

  @POST
  @Path("/resourceWithInheritance")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response post(ResourceWithInheritance resourceWithInheritance) {
    return Response.ok().build();
  }
}
