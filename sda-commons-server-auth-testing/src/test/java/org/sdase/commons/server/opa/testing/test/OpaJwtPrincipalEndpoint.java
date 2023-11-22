package org.sdase.commons.server.opa.testing.test;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import org.sdase.commons.server.opa.OpaJwtPrincipal;

@Path("/principal")
@Produces(APPLICATION_JSON)
public class OpaJwtPrincipalEndpoint implements Feature {

  @Context private OpaJwtPrincipal opaJwtPrincipal;

  @GET
  @Path("/constraints")
  public OpaJwtPrincipalInjectApp.Constraints getOpaJwtPrincipalConstraints() {
    return opaJwtPrincipal.getConstraintsAsEntity(OpaJwtPrincipalInjectApp.Constraints.class);
  }

  @GET
  @Path("/token")
  public String getOpaJwtPrincipalToken() {
    return opaJwtPrincipal.getJwt();
  }

  @Override
  public boolean configure(FeatureContext context) {
    return true;
  }
}
