package org.sdase.commons.client.jersey.oidc;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import org.apache.commons.lang3.StringUtils;
import org.sdase.commons.client.jersey.HttpClientConfiguration;
import org.sdase.commons.client.jersey.oidc.cache.CacheConfiguration;

/**
 * Supports two grant types:
 *
 * <h2>client_credentials</h2>
 *
 * Can be used for technical users. You only need to provide {@link #clientId} and {@link
 * #clientSecret}.
 *
 * <h2>password</h2>
 *
 * In addition to your client credentials you can specify {@link #username} and {@link #password}
 */
public class OidcConfiguration {

  private static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
  private static final String GRANT_TYPE_PASSWORD = "password";

  private boolean disabled;

  /**
   * Supported grant types:
   *
   * <ul>
   *   <li>client_credentials
   *   <li>password
   * </ul>
   */
  private String grantType = GRANT_TYPE_CLIENT_CREDENTIALS;

  private String clientId;

  private String clientSecret;

  /** Store 'client_id' + 'client_secret' in the authorization header using Basic auth. */
  private boolean useAuthHeader;

  private String username;

  private String password;

  private CacheConfiguration cache = new CacheConfiguration();

  /**
   * Contains the URL to the OpenID provider configuration document, e.g. {@code
   * https://<keycloak.domain>/auth/realms/<realm>} for Keycloak
   */
  private String issuerUrl;

  @Valid private HttpClientConfiguration httpClient = new HttpClientConfiguration();

  public boolean isDisabled() {
    return disabled;
  }

  public OidcConfiguration setDisabled(boolean disabled) {
    this.disabled = disabled;
    return this;
  }

  public String getGrantType() {
    return grantType;
  }

  public OidcConfiguration setGrantType(String grantType) {
    this.grantType = grantType;
    return this;
  }

  public String getClientId() {
    return clientId;
  }

  public OidcConfiguration setClientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public OidcConfiguration setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
    return this;
  }

  public boolean isUseAuthHeader() {
    return useAuthHeader;
  }

  public OidcConfiguration setUseAuthHeader(boolean useAuthHeader) {
    this.useAuthHeader = useAuthHeader;
    return this;
  }

  public String getUsername() {
    return username;
  }

  public OidcConfiguration setUsername(String username) {
    this.username = username;
    return this;
  }

  public String getPassword() {
    return password;
  }

  public OidcConfiguration setPassword(String password) {
    this.password = password;
    return this;
  }

  public String getIssuerUrl() {
    return issuerUrl;
  }

  public OidcConfiguration setIssuerUrl(String issuerUrl) {
    this.issuerUrl = issuerUrl;
    return this;
  }

  public HttpClientConfiguration getHttpClient() {
    return httpClient;
  }

  public OidcConfiguration setHttpClient(HttpClientConfiguration httpClient) {
    this.httpClient = httpClient;
    return this;
  }

  public CacheConfiguration getCache() {
    return cache;
  }

  public OidcConfiguration setCache(CacheConfiguration cache) {
    this.cache = cache;
    return this;
  }

  @AssertTrue(message = "httpClient is required")
  public boolean isHttpClientValid() {
    if (disabled) {
      return true;
    }

    return httpClient != null;
  }

  @AssertTrue(message = "grantType must be one of client_credentials or password")
  public boolean isGrantTypeValid() {
    if (disabled) {
      return true;
    }

    return GRANT_TYPE_CLIENT_CREDENTIALS.equals(grantType) || GRANT_TYPE_PASSWORD.equals(grantType);
  }

  @AssertTrue(message = "clientId is required")
  public boolean isClientIdValid() {
    if (disabled) {
      return true;
    }

    return StringUtils.isNotBlank(clientId);
  }

  @AssertTrue(message = "clientSecret is required")
  public boolean isClientSecretValid() {
    if (disabled) {
      return true;
    }

    return StringUtils.isNotBlank(clientSecret);
  }

  @AssertTrue(message = "username is required for grant type password")
  public boolean isUsernameValid() {
    if (disabled) {
      return true;
    }
    if (!GRANT_TYPE_PASSWORD.equals(grantType)) {
      return true;
    }

    return StringUtils.isNotBlank(username);
  }

  @AssertTrue(message = "password is required for grant type password")
  public boolean isPasswordValid() {
    if (disabled) {
      return true;
    }
    if (!GRANT_TYPE_PASSWORD.equals(grantType)) {
      return true;
    }

    return StringUtils.isNotBlank(password);
  }
}
