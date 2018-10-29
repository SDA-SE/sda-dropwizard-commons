package com.sdase.commons.server.auth.service;

import com.sdase.commons.server.auth.JwtPrincipal;
import io.dropwizard.auth.Authorizer;

/**
 * A stragety that authorizes {@link JwtPrincipal}s based on their claims.
 */
public class JwtAuthorizer implements Authorizer<JwtPrincipal> {

   private boolean disabled;

   public JwtAuthorizer(boolean disabled) {
      this.disabled = disabled;
   }

   @Override
   public boolean authorize(JwtPrincipal principal, String role) {
      if (disabled) { // NOSONAR
         return true;
      }
      // TODO Authorization: Later on we may implement a role based system here (e.g. taking claims from the JWT)
      return false;
   }
}
