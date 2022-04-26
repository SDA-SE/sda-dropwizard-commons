package org.sdase.commons.server.auth.service;

import com.auth0.jwt.interfaces.Claim;
import java.util.Map;

@FunctionalInterface
public interface TokenAuthorizer {

  /**
   * Reads the Claims from the given {@code authorizationToken}.
   *
   * @param authorizationToken a JWT that may contain claims.
   * @return the claims in the {@code authorizationToken} as {@link Map} with the key as name of the
   *     claim and the value as {@link Claim} value.
   */
  Map<String, Claim> auth(String authorizationToken);
}
