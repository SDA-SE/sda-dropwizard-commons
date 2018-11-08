package org.sdase.commons.server.auth;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import com.auth0.jwt.interfaces.Claim;

/**
 * A principal that is created from a verified JWT.
 */
public class JwtPrincipal implements Principal {

   private static final String DEFAULT_NAME = JwtPrincipal.class.getSimpleName();

   private String name;

   private String jwt;

   private Map<String, Claim> claims;

   private JwtPrincipal(String name, String jwt, Map<String, Claim> claims) {
      this.name = name;
      this.jwt = jwt;
      this.claims = claims;
   }

   /**
    * @return an existing but unverified principal without token and claims. Should only be used if an user is
    *         authorized because authorization is disabled
    */
   public static JwtPrincipal emptyPrincipal() {
      return new JwtPrincipal(null, null, new HashMap<>());
   }

   /**
    * @param jwt The token this Principal is created from. May be required to pass it to other services.
    * @param claims The claims in the verified {@code jwt}.
    */
   public static JwtPrincipal verifiedPrincipal(String jwt, Map<String, Claim> claims) {
      return new JwtPrincipal(DEFAULT_NAME, jwt, claims);
   }

   @Override
   public String getName() {
      return name;
   }

   public String getJwt() {
      return jwt;
   }

   public Map<String, Claim> getClaims() {
      return claims;
   }
}
