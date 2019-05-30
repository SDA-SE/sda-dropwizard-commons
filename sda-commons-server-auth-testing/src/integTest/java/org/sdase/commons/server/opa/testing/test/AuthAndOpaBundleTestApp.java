package org.sdase.commons.server.opa.testing.test;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.io.IOException;
import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.sdase.commons.server.auth.AuthBundle;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.opa.OpaBundle;
import org.sdase.commons.server.opa.OpaJwtPrincipal;

public class AuthAndOpaBundleTestApp extends Application<AuthAndOpaBundeTestAppConfiguration> {

   @Override
   public void initialize(Bootstrap<AuthAndOpaBundeTestAppConfiguration> bootstrap) {
      bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
      bootstrap
            .addBundle(AuthBundle
                  .builder()
                  .withAuthConfigProvider(AuthAndOpaBundeTestAppConfiguration::getAuth)
                  .withExternalAuthorization()
                  .build());
      bootstrap
            .addBundle(OpaBundle.builder().withOpaConfigProvider(AuthAndOpaBundeTestAppConfiguration::getOpa).build());
   }

   @Override
   public void run(AuthAndOpaBundeTestAppConfiguration configuration, Environment environment) {
      environment.jersey().register(Endpoint.class);
   }

   @Path("/")
   public static class Endpoint {

      @Context
      SecurityContext securityContext;

      @GET
      @Path("resources")
      public Response get() throws IOException {
         OpaJwtPrincipal principal = (OpaJwtPrincipal) securityContext.getUserPrincipal();

         PrincipalInfo result = new PrincipalInfo()
               .setName(principal.getName())
               .setJwt(principal.getJwt())
               .setConstraints(principal.getConstraintsAsEntity(ConstraintModel.class));

         return Response.ok(result, MediaType.APPLICATION_JSON_TYPE).build();
      }

   }
}
