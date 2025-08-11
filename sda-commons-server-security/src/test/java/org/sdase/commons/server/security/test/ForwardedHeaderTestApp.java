package org.sdase.commons.server.security.test;

import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.jackson.JacksonConfigurationBundle;
import org.sdase.commons.server.security.SecurityBundle;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class ForwardedHeaderTestApp extends Application<Configuration> {

  public static final String RESOURCE_PATH = "test-resources";
  public static final String RESOURCE_ID = "123";

  public static void main(String[] args) throws Exception {
    new ForwardedHeaderTestApp().run(args);
  }

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
    bootstrap.addBundle(SecurityBundle.builder().build());
    bootstrap.addBundle(JacksonConfigurationBundle.builder().build());
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    environment.jersey().register(this);
  }

  @POST
  @Path(RESOURCE_PATH)
  public Response createResourceWithForwardedHeader(@Context UriInfo uriInfo) {
    var location = uriInfo.getBaseUriBuilder().path(RESOURCE_PATH).path(RESOURCE_ID).build();
    return Response.created(location).build();
  }
}
