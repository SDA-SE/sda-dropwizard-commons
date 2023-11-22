package org.sdase.commons.server.jackson.test;

import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.openapitools.jackson.dataformat.hal.HALLink;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import org.sdase.commons.server.dropwizard.ContextAwareEndpoint;
import org.sdase.commons.server.jackson.JacksonConfigurationBundle;

@Path("")
public class JacksonConfigurationNoFieldFilterTestApp extends Application<Configuration>
    implements ContextAwareEndpoint {

  @Context private UriInfo uriInfo;

  public static void main(String[] args) throws Exception {
    new JacksonConfigurationNoFieldFilterTestApp().run(args);
  }

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    bootstrap.addBundle(JacksonConfigurationBundle.builder().withoutFieldFilter().build());
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    environment.jersey().register(this);
  }

  @GET
  @Path("/people/jdoe")
  @Produces(MediaType.APPLICATION_JSON)
  public PersonResource getJohnDoe() {
    URI self =
        uriInfo
            .getBaseUriBuilder()
            .path(JacksonConfigurationNoFieldFilterTestApp.class, "getJohnDoe")
            .build();
    return new PersonResource()
        .setFirstName("John")
        .setLastName("Doe")
        .setNickName("Johnny")
        .setSelf(new HALLink.Builder(self).build());
  }
}
