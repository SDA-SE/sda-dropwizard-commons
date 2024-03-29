package org.sdase.commons.server.auth.service;

import com.auth0.jwt.interfaces.Claim;
import io.dropwizard.auth.Authenticator;
import java.util.Map;
import java.util.Optional;
import org.sdase.commons.server.auth.JwtPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An authenticator is a strategy class which, given a JWT as client-provided credentials, verify
 * JWT against given Algorithm and possibly returns a principal (i.e., the person or entity on
 * behalf of whom your service will do something).
 */
public class JwtAuthenticator implements Authenticator<Optional<String>, JwtPrincipal> {
  private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticator.class);

  private TokenAuthorizer tokenAuthorizer;
  private boolean disabled;

  public JwtAuthenticator(TokenAuthorizer tokenAuthorizer, boolean disabled) {
    this.tokenAuthorizer = tokenAuthorizer;
    this.disabled = disabled;
  }

  @Override
  public Optional<JwtPrincipal> authenticate(Optional<String> credentials) {
    if (disabled) {
      LOGGER.warn(
          "Authentication is disabled. This setting shall never be active in production. To fix this warning "
              + "remove configuration 'auth.disableAuth'.");
      return Optional.of(JwtPrincipal.emptyPrincipal());
    }
    if (!credentials.isPresent()) {
      LOGGER.info("No access token received");
      return Optional.empty();
    }
    final Map<String, Claim> claims = tokenAuthorizer.auth(credentials.get());
    return Optional.of(JwtPrincipal.verifiedPrincipal(credentials.get(), claims));
  }
}
