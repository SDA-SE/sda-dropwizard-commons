package org.sdase.commons.server.opa.internal;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import org.glassfish.hk2.api.Factory;
import org.sdase.commons.server.opa.OpaJwtPrincipal;

/** A factory that is able to provide the {@link OpaJwtPrincipal} in the request context. */
public class OpaJwtPrincipalFactory implements Factory<OpaJwtPrincipal> {

  private SecurityContext securityContext;

  @Inject
  public OpaJwtPrincipalFactory(SecurityContext securityContext) {
    this.securityContext = securityContext;
  }

  @Override
  public OpaJwtPrincipal provide() {
    Principal userPrincipal = securityContext.getUserPrincipal();
    if (userPrincipal instanceof OpaJwtPrincipal) {
      return (OpaJwtPrincipal) userPrincipal;
    }
    return null;
  }

  @Override
  public void dispose(OpaJwtPrincipal instance) {
    // ignored
  }
}
