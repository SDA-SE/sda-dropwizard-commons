package org.sdase.commons.server.opa.internal;

import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.sdase.commons.server.opa.OpaBundle;
import org.sdase.commons.server.opa.OpaJwtPrincipal;

/**
 * Principal for @{@link javax.ws.rs.core.SecurityContext} that optionally contains a JWT and a set
 * of constraints as JSON object string.
 *
 * <p>The {@code OpaJwtPrincipal} can be injected as field in endpoint implementations using {@link
 * javax.ws.rs.core.Context} when the {@link OpaBundle} is used to setup the open policy agent
 * configuration.
 */
public class OpaJwtPrincipalImpl implements OpaJwtPrincipal {

  private String name;
  private String jwt;
  private Map<String, Claim> claims;
  private JsonNode constraints;
  private ObjectMapper om;

  public OpaJwtPrincipalImpl(
      String name, String jwt, Map<String, Claim> claims, JsonNode constraints, ObjectMapper om) {
    this.name = name;
    this.jwt = jwt;
    this.claims = claims;
    this.constraints = constraints;
    this.om = om;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getJwt() {
    return jwt;
  }

  @Override
  public Map<String, Claim> getClaims() {
    return claims;
  }

  @Override
  public String getConstraints() {
    return constraints.toString();
  }

  @Override
  public <T> T getConstraintsAsEntity(Class<T> resultType) {
    if (constraints != null) {
      return om.convertValue(constraints, resultType);
    } else {
      return null;
    }
  }
}
