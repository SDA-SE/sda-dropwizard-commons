package org.sdase.commons.server.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.sdase.commons.server.auth.error.JwtAuthException;
import org.sdase.commons.server.auth.key.LoadedPublicKey;
import org.sdase.commons.server.auth.key.PublicKeyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthService implements TokenAuthorizer {

  private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);

  private PublicKeyLoader publicKeyLoader;

  private long leeway;

  public AuthService(PublicKeyLoader publicKeyLoader, long leeway) {
    Validate.notNull(publicKeyLoader);
    Validate.inclusiveBetween(0, Long.MAX_VALUE, leeway);
    this.publicKeyLoader = publicKeyLoader;
    this.leeway = leeway;
  }

  @Override
  public Map<String, Claim> auth(String authorizationToken) {
    try {
      String keyId = JWT.decode(authorizationToken).getKeyId();
      if (keyId == null) {
        // check all keys without id
        List<LoadedPublicKey> keysWithoutId = publicKeyLoader.getKeysWithoutId();
        if (keysWithoutId.size() > 1) {
          LOG.warn("Verifying token without kid trying {} public keys", keysWithoutId.size());
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
        LoadedPublicKey loadedPublicKey = publicKeyLoader.getLoadedPublicKey(keyId);

        if (loadedPublicKey == null) {
          LOG.error("No key found for verification, matching the requested kid {}", keyId);
          throw new JwtAuthException("Could not verify JWT with the requested kid.");
        }

        DecodedJWT jwt =
            verifyJwtSignature(authorizationToken, loadedPublicKey)
                .orElseThrow(() -> new JwtAuthException("Verifying token failed"));
        return jwt.getClaims();
      }
    } catch (JWTVerificationException e) {
      throw new JwtAuthException(e);
    }
  }

  private static Algorithm resolveAlgorithm(LoadedPublicKey loadedPublicKey) {
    switch (loadedPublicKey.getSigAlgorithm()) {
      case "RS256":
        return Algorithm.RSA256((RSAPublicKey) loadedPublicKey.getPublicKey(), null);
      case "RS384":
        return Algorithm.RSA384((RSAPublicKey) loadedPublicKey.getPublicKey(), null);
      case "RS512":
        return Algorithm.RSA512((RSAPublicKey) loadedPublicKey.getPublicKey(), null);
      case "ES256":
        return Algorithm.ECDSA256((ECPublicKey) loadedPublicKey.getPublicKey(), null);
      case "ES384":
        return Algorithm.ECDSA384((ECPublicKey) loadedPublicKey.getPublicKey(), null);
      case "ES512":
        return Algorithm.ECDSA512((ECPublicKey) loadedPublicKey.getPublicKey(), null);
      default:
        throw new JwtAuthException(
            "Unsupported algorithm :'" + loadedPublicKey.getSigAlgorithm() + "'");
    }
  }

  private Optional<DecodedJWT> verifyJwtSignature(
      String authorizationToken, LoadedPublicKey loadedPublicKey) {
    try {

      Verification jwtVerification =
          JWT.require(resolveAlgorithm(loadedPublicKey)).acceptLeeway(leeway);

      if (StringUtils.isNotBlank(loadedPublicKey.getRequiredIssuer())) {
        jwtVerification = jwtVerification.withIssuer(loadedPublicKey.getRequiredIssuer());
      }

      DecodedJWT jwt = jwtVerification.build().verify(authorizationToken);
      return Optional.of(jwt);

    } catch (TokenExpiredException e) {
      LOG.warn("Verifying token failed.", e);
      return Optional.empty();
    } catch (JWTVerificationException e) {
      LOG.error("Verifying token failed.", e);
      return Optional.empty();
    }
  }
}
