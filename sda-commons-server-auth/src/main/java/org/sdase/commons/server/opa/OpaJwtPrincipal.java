package org.sdase.commons.server.opa;

import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;

/**
 * Principal for @{@link javax.ws.rs.core.SecurityContext} that optionally contains a JWT and
 * a set of constraints as JSON object string.
 */
public class OpaJwtPrincipal implements Principal {

  private static final String DEFAULT_NAME = OpaJwtPrincipal.class.getSimpleName();

  private String name;
  private String jwt;
  private Map<String, Claim> claims;
  private String constraints;
  private ObjectMapper om;

  private OpaJwtPrincipal(String name, String jwt, Map<String, Claim> claims, String constraints, ObjectMapper om) {
    this.name = name;
    this.jwt = jwt;
    this.claims = claims;
    this.constraints = constraints;
    this.om = om;
  }

  /**
   * @param jwt The token this Principal is created from. May be required to pass it to other services.
   * @param claims The claims in the verified {@code jwt}.
   * @param constraints Authorization details used within the service for limiting result data
   */
  public static OpaJwtPrincipal create(String jwt, Map<String, Claim> claims, String constraints, ObjectMapper om) {
    return new OpaJwtPrincipal(DEFAULT_NAME, jwt, claims, constraints, om);
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * @return the JWT as string
   */
  public String getJwt() {
    return jwt;
  }

  /**
   * @return map with the claims decoded from the JWT
   */
  public Map<String, Claim> getClaims() {
    return claims;
  }

  /**
   * @return the constraint object as JSON String
   */
  public String getConstraints() {
    return constraints;
  }

  /**
   * returns the constraint as Object. The object type must match the response from OPA sidecar
   * @param resultType Result class to that the constraint string is parsed
   * @param <T> type for correct casting
   * @return the object or null if no constraint exists
   * @throws IOException exception of parsing fails
   */
  public <T> T getConstraintsAsEntity(Class<T> resultType) throws IOException {
    if (!Strings.isNullOrEmpty(constraints)) {
      return om.readValue(constraints, resultType);
    } else {
      return null;
    }
  }
}
