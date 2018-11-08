package org.sdase.commons.server.auth.testing.test;

import com.auth0.jwt.interfaces.Claim;
import org.sdase.commons.server.auth.JwtPrincipal;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Map;

import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/admin")
@RolesAllowed("ADMIN")
public class RolesAwareEndpoint {

   @Context
   private SecurityContext securityContext;

   @GET
   @Produces(APPLICATION_JSON)
   public Response returnClaims() {
      JwtPrincipal jwtPrincipal = (JwtPrincipal) securityContext.getUserPrincipal();
      Map<String, Claim> claims = jwtPrincipal.getClaims();
      Map<String, String> claimsAsString = claims.entrySet().stream()
            .collect(toMap(Map.Entry::getKey, e -> e.getValue().asString()));
      return Response.ok(claimsAsString).build();
   }

}
