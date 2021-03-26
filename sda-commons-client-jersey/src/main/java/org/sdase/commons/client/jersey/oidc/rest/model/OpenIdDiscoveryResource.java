package org.sdase.commons.client.jersey.oidc.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A resource providing information according to <a href=
 * "https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata">OpenID spec 3</a>.
 */
public class OpenIdDiscoveryResource {
  @JsonProperty("token_endpoint")
  private String tokenEndpoint;

  public String getTokenEndpoint() {
    return tokenEndpoint;
  }

  OpenIdDiscoveryResource setTokenEndpoint(String tokenEndpoint) {
    this.tokenEndpoint = tokenEndpoint;
    return this;
  }
}
