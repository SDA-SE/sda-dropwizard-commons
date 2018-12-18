package org.sdase.commons.server.security.test;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.jackson.JacksonConfigurationBundle;
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
      bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build()); // needed to update the config in tests
      bootstrap.addBundle(SecurityBundle.builder().build());
      if (!"true".equals(System.getenv("DISABLE_JACKSON_CONFIGURATION"))) {
         bootstrap.addBundle(JacksonConfigurationBundle.builder().build()); // enables custom error handlers
      }
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

   @GET
   @Path("throw")
   @Produces("application/json")
   public String throwException() {
      // intentionally use RuntimeException to check error handler for the basics
      throw new RuntimeException("An always thrown exception"); // NOSONAR
   }

}
