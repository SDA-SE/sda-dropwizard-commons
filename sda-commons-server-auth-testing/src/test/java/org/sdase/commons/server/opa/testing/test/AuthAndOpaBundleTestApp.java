package org.sdase.commons.server.opa.testing.test;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.sdase.commons.server.auth.AuthBundle;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.opa.OpaBundle;
import org.sdase.commons.server.opa.OpaJwtPrincipal;

public class AuthAndOpaBundleTestApp extends Application<AuthAndOpaBundeTestAppConfiguration> {

  @Override
  public void initialize(Bootstrap<AuthAndOpaBundeTestAppConfiguration> bootstrap) {
    bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
    bootstrap.addBundle(
        AuthBundle.builder()
            .withAuthConfigProvider(AuthAndOpaBundeTestAppConfiguration::getAuth)
            .withExternalAuthorization()
            .build());
    bootstrap.addBundle(
        OpaBundle.builder()
            .withOpaConfigProvider(AuthAndOpaBundeTestAppConfiguration::getOpa)
            .build());
  }

  @Override
  public void run(AuthAndOpaBundeTestAppConfiguration configuration, Environment environment) {
    environment.jersey().register(Endpoint.class);
  }

  @Path("/")
  public static class Endpoint {

    @Context SecurityContext securityContext;

    @GET
    @Path("resources")
    public Response get() {
      OpaJwtPrincipal principal = (OpaJwtPrincipal) securityContext.getUserPrincipal();

      PrincipalInfo result =
          new PrincipalInfo()
              .setName(principal.getName())
              .setJwt(principal.getJwt())
              .setSub(
                  principal.getClaims() == null
                      ? null
                      : principal.getClaims().get("sub").asString())
              .setConstraints(principal.getConstraintsAsEntity(ConstraintModel.class))
              .setConstraintsJson(principal.getConstraints());

      return Response.ok(result, MediaType.APPLICATION_JSON_TYPE).build();
    }
  }
}
