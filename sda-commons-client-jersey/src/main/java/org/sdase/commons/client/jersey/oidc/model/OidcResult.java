package org.sdase.commons.client.jersey.oidc.model;

public class OidcResult {
  private static final String BEARER = "Bearer";

  private String accessToken;
  private OidcState state;

  public String getAccessToken() {
    return accessToken;
  }

  public String getBearerToken() {
    return String.format("%s %s", BEARER, accessToken);
  }

  public OidcResult setAccessToken(String accessToken) {
    this.accessToken = accessToken;
    return this;
  }

  public OidcState getState() {
    return state;
  }

  public OidcResult setState(OidcState state) {
    this.state = state;
    return this;
  }
}
