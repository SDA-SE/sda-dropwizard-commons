package org.sdase.commons.server.opa.testing.test;

import java.io.IOException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.sdase.commons.server.opa.OpaJwtPrincipal;

@Path("/")
public class Endpoint {

  @Context SecurityContext securityContext;

  @GET
  @Path("resources")
  public Response get() throws IOException {
    OpaJwtPrincipal principal = (OpaJwtPrincipal) securityContext.getUserPrincipal();

    PrincipalInfo result =
        new PrincipalInfo()
            .setName(principal.getName())
            .setJwt(principal.getJwt())
            .setConstraints(principal.getConstraintsAsEntity(ConstraintModel.class));

    return Response.ok(result, MediaType.APPLICATION_JSON_TYPE).build();
  }

  @POST
  @Path("resources/actions")
  public Response post() throws IOException { // NOSONAR
    OpaJwtPrincipal principal = (OpaJwtPrincipal) securityContext.getUserPrincipal();

    PrincipalInfo result =
        new PrincipalInfo()
            .setName(principal.getName())
            .setJwt(principal.getJwt())
            .setConstraints(principal.getConstraintsAsEntity(ConstraintModel.class));

    return Response.ok(result, MediaType.APPLICATION_JSON_TYPE).build();
  }

  @POST
  @Path("excluded/resources")
  public Response postExcluded() throws IOException { // NOSONAR
    OpaJwtPrincipal principal = (OpaJwtPrincipal) securityContext.getUserPrincipal();

    PrincipalInfo result =
        new PrincipalInfo()
            .setName(principal.getName())
            .setJwt(principal.getJwt())
            .setConstraints(principal.getConstraintsAsEntity(ConstraintModel.class));

    return Response.ok(result, MediaType.APPLICATION_JSON_TYPE).build();
  }
}
