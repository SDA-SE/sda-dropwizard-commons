package org.sdase.commons.server.auth.testing.test;

import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.auth0.jwt.interfaces.Claim;
import java.util.Map;
import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.sdase.commons.server.auth.JwtPrincipal;

@Path("/secure")
@PermitAll
public class SecureEndPoint {

  @Context private SecurityContext securityContext;

  @GET
  @Produces(APPLICATION_JSON)
  public Response returnClaims() {
    JwtPrincipal jwtPrincipal = (JwtPrincipal) securityContext.getUserPrincipal();
    Map<String, Claim> claims = jwtPrincipal.getClaims();
    Map<String, String> claimsAsString =
        claims.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> e.getValue().asString()));
    return Response.ok(claimsAsString).build();
  }
}
