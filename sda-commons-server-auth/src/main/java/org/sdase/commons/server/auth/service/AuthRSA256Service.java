package org.sdase.commons.server.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.sdase.commons.server.auth.error.JwtAuthException;
import org.sdase.commons.server.auth.key.RsaPublicKeyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthRSA256Service implements AuthService {

  private static Logger log = LoggerFactory.getLogger(AuthRSA256Service.class);

  private RsaPublicKeyLoader rsaPublicKeyLoader;

  private long leeway;

  public AuthRSA256Service(RsaPublicKeyLoader rsaPublicKeyLoader, long leeway) {
    Validate.notNull(rsaPublicKeyLoader);
    Validate.inclusiveBetween(0, Long.MAX_VALUE, leeway);
    this.rsaPublicKeyLoader = rsaPublicKeyLoader;
    this.leeway = leeway;
  }

  @Override
  public Map<String, Claim> auth(String authorizationToken) {
    try {
      String keyId = JWT.decode(authorizationToken).getKeyId();
      if (keyId == null) {
        // check all keys without id
        List<RSAPublicKey> keysWithoutId = rsaPublicKeyLoader.getKeysWithoutId();
        if (keysWithoutId.size() > 1) {
          log.warn("Verifying token without kid trying {} public keys", keysWithoutId.size());
        }
        Collections.reverse(keysWithoutId);
        return keysWithoutId.stream()
            .map(k -> verifyJwtSignature(authorizationToken, k))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst()
            .orElseThrow(() -> new JwtAuthException("Could not verify JWT without kid."))
            .getClaims();
      } else {
        RSAPublicKey key = rsaPublicKeyLoader.getKey(keyId);

        if (key == null) {
          log.error("No key found for verification, matching the requested kid {}", keyId);
          throw new JwtAuthException("Could not verify JWT with the requested kid.");
        }

        DecodedJWT jwt =
            verifyJwtSignature(authorizationToken, key)
                .orElseThrow(() -> new JwtAuthException("Verifying token failed"));
        return jwt.getClaims();
      }
    } catch (JWTVerificationException e) {
      throw new JwtAuthException(e);
    }
  }

  private Optional<DecodedJWT> verifyJwtSignature(
      String authorizationToken, RSAPublicKey publicKey) {
    try {
      DecodedJWT jwt =
          JWT.require(Algorithm.RSA256(publicKey, null))
              .acceptLeeway(leeway)
              .build()
              .verify(authorizationToken);
      return Optional.of(jwt);
    } catch (TokenExpiredException e) {
      log.warn("Verifying token failed.", e);
      return Optional.empty();
    } catch (JWTVerificationException e) {
      log.error("Verifying token failed.", e);
      return Optional.empty();
    }
  }
}
