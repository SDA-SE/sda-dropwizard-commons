package org.sdase.commons.server.opa.testing.test;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
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
