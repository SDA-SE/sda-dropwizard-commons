package org.sdase.commons.server.jackson.test;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.openapitools.jackson.dataformat.hal.HALLink;
import java.net.URI;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import org.sdase.commons.server.jackson.JacksonConfigurationBundle;

@Path("")
public class JacksonConfigurationNoFieldFilterTestApp extends Application<Configuration> {

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
