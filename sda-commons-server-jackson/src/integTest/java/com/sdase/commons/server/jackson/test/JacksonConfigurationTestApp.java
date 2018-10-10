package com.sdase.commons.server.jackson.test;

import com.sdase.commons.server.jackson.JacksonConfigurationBundle;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.openapitools.jackson.dataformat.hal.HALLink;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Path("")
public class JacksonConfigurationTestApp extends Application<Configuration> {

   @Context
   private UriInfo uriInfo;

   public static void main(String[] args) throws Exception {
      new JacksonConfigurationTestApp().run(args);
   }

   @Override
   public void initialize(Bootstrap<Configuration> bootstrap) {
      bootstrap.addBundle(JacksonConfigurationBundle.builder().build());
   }

   @Override
   public void run(Configuration configuration, Environment environment) {
      environment.jersey().register(this);
   }

   @GET
   @Path("/jdoe")
   @Produces(MediaType.APPLICATION_JSON)
   public PersonResource getJohnDoe() {
      URI self = uriInfo.getBaseUriBuilder().path(JacksonConfigurationTestApp.class, "getJohnDoe").build();
      return new PersonResource()
            .setFirstName("John")
            .setLastName("Doe")
            .setNickName("Johnny")
            .setSelf(new HALLink.Builder(self).build());
   }
}
