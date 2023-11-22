package org.sdase.commons.server.auth.testing.test;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.stream.Collectors.toMap;

import com.auth0.jwt.interfaces.Claim;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.Map;
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
