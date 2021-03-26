package org.sdase.commons.client.jersey.oidc.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An access token resource according to <a
 * href="https://tools.ietf.org/html/rfc6749#section-5.1">RFC 6749 section 5.1</a>
 */
public class TokenResource {
  @JsonProperty("token_type")
  private String tokenType;

  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("expires_in")
  private long accessTokenExpiresInSeconds;

  public String getTokenType() {
    return tokenType;
  }

  public TokenResource setTokenType(String tokenType) {
    this.tokenType = tokenType;
    return this;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public TokenResource setAccessToken(String accessToken) {
    this.accessToken = accessToken;
    return this;
  }

  public long getAccessTokenExpiresInSeconds() {
    return accessTokenExpiresInSeconds;
  }

  public TokenResource setAccessTokenExpiresInSeconds(long accessTokenExpiresInSeconds) {
    this.accessTokenExpiresInSeconds = accessTokenExpiresInSeconds;
    return this;
  }
}
