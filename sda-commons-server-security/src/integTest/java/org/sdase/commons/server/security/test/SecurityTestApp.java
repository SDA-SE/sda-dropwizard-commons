package org.sdase.commons.server.security.test;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.security.SecurityBundle;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

@Path("")
public class SecurityTestApp extends Application<Configuration> {

   public static void main(String[] args) throws Exception {
      new SecurityTestApp().run(args);
   }

   @Override
   public void initialize(Bootstrap<Configuration> bootstrap) {
      bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
      bootstrap.addBundle(SecurityBundle.builder().build());
   }

   @Override
   public void run(Configuration configuration, Environment environment) {
      environment.jersey().register(this);
   }

   @GET
   @Path("caller")
   @Produces("text/plain")
   public String identifyCaller(@Context HttpServletRequest request) {
      return request.getRemoteAddr();
   }

   @GET
   @Path("link")
   @Produces("text/plain")
   public String produceLink(@Context UriInfo uriInfo) {
      return uriInfo.getBaseUri().toASCIIString();
   }

}
