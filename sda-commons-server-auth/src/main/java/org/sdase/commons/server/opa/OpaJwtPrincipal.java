package org.sdase.commons.server.opa;

import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.Map;
import org.sdase.commons.server.opa.internal.OpaJwtPrincipalImpl;

/**
 * Principal for @{@link javax.ws.rs.core.SecurityContext} that optionally contains a JWT and a set
 * of constraints as JSON object string.
 *
 * <p>The {@code OpaJwtPrincipal} can be injected as field in endpoint implementations using {@link
 * javax.ws.rs.core.Context} when the {@link OpaBundle} is used to setup the open policy agent
 * configuration.
 */
public interface OpaJwtPrincipal extends Principal {

  /**
   * @param jwt The token this Principal is created from. May be required to pass it to other
   *     services.
   * @param claims The claims in the verified {@code jwt}.
   * @param om The Object Mapper to use to decode the constraints.
   * @param constraints Authorization details used within the service for limiting result data
   * @return the principal that contains a jwt token, claims, and constraints that can be decoded
   */
  static OpaJwtPrincipalImpl create(
      String jwt, Map<String, Claim> claims, JsonNode constraints, ObjectMapper om) {
    String defaultName = OpaJwtPrincipal.class.getSimpleName();
    return new OpaJwtPrincipalImpl(defaultName, jwt, claims, constraints, om);
  }

  /** @return the JWT as string */
  String getJwt();

  /** @return map with the claims decoded from the JWT */
  Map<String, Claim> getClaims();

  /** @return the constraint object as JSON String */
  String getConstraints();

  /**
   * returns the constraint as Object. The object type must match the response from OPA sidecar
   *
   * @param resultType Result class to that the constraint string is parsed
   * @param <T> type for correct casting
   * @return the object or null if no constraint exists
   */
  <T> T getConstraintsAsEntity(Class<T> resultType);
}
