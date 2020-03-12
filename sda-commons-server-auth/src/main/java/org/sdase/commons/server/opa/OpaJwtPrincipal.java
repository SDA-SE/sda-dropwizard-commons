package org.sdase.commons.server.opa;

import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.Map;

/**
 * Principal for @{@link javax.ws.rs.core.SecurityContext} that optionally contains a JWT and a set
 * of constraints as JSON object string.
 */
public class OpaJwtPrincipal implements Principal {

  private static final String DEFAULT_NAME = OpaJwtPrincipal.class.getSimpleName();

  private String name;
  private String jwt;
  private Map<String, Claim> claims;
  private JsonNode constraints;
  private ObjectMapper om;

  private OpaJwtPrincipal(
      String name, String jwt, Map<String, Claim> claims, JsonNode constraints, ObjectMapper om) {
    this.name = name;
    this.jwt = jwt;
    this.claims = claims;
    this.constraints = constraints;
    this.om = om;
  }

  /**
   * @param jwt The token this Principal is created from. May be required to pass it to other
   *     services.
   * @param claims The claims in the verified {@code jwt}.
   * @param om The Object Mapper to use to decode the constraints.
   * @param constraints Authorization details used within the service for limiting result data
   * @return the principal that contains a jwt token, claims, and constraints that can be decoded
   */
  public static OpaJwtPrincipal create(
      String jwt, Map<String, Claim> claims, JsonNode constraints, ObjectMapper om) {
    return new OpaJwtPrincipal(DEFAULT_NAME, jwt, claims, constraints, om);
  }

  @Override
  public String getName() {
    return name;
  }

  /** @return the JWT as string */
  public String getJwt() {
    return jwt;
  }

  /** @return map with the claims decoded from the JWT */
  public Map<String, Claim> getClaims() {
    return claims;
  }

  /** @return the constraint object as JSON String */
  public String getConstraints() {
    return constraints.toString();
  }

  /**
   * returns the constraint as Object. The object type must match the response from OPA sidecar
   *
   * @param resultType Result class to that the constraint string is parsed
   * @param <T> type for correct casting
   * @return the object or null if no constraint exists
   */
  public <T> T getConstraintsAsEntity(Class<T> resultType) {
    if (constraints != null) {
      return om.convertValue(constraints, resultType);
    } else {
      return null;
    }
  }
}
