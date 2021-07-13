package org.sdase.commons.server.auth.config;

import java.net.URI;

/** Defines a location of keys. */
public class KeyLocation {

  /**
   * Uri leading to
   *
   * <ul>
   *   <li>a JSON Web Key Set,
   *   <li>a OpenID provider base Uri or
   *   <li>a key file in PEM format.
   * </ul>
   *
   * <p>The type of the Uri depends on the {@link #type}.
   *
   * <p>Further information:
   *
   * <ul>
   *   <li><a
   *       href="https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata">jwks_uri
   *       in OpenID spec</a>
   *   <li><a
   *       href="https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfigurationRequest">OpenID
   *       Provider</a>
   * </ul>
   */
  private URI location;

  /** The type of the {@link #location} that defines how the certificate is loaded. */
  private KeyUriType type;

  /**
   * Optional <a href="https://tools.ietf.org/html/draft-ietf-jose-json-web-key-41#section-4.5">Key
   * Id</a> used only if the {@link #type} is {@link KeyUriType#PEM}.
   */
  private String pemKeyId;

  /**
   * Optional <a href="https://openid.net/specs/openid-connect-core-1_0.html#IDToken">iss</a> that
   * specifies the required issuer of the jwt token, if not empty.
   */
  private String requiredIssuer;

  public URI getLocation() {
    return location;
  }

  public KeyLocation setLocation(URI location) {
    this.location = location;
    return this;
  }

  public KeyUriType getType() {
    return type;
  }

  public KeyLocation setType(KeyUriType type) {
    this.type = type;
    return this;
  }

  public String getPemKeyId() {
    return pemKeyId;
  }

  public KeyLocation setPemKeyId(String pemKeyId) {
    this.pemKeyId = pemKeyId;
    return this;
  }

  public String getRequiredIssuer() {
    return requiredIssuer;
  }

  public KeyLocation setRequiredIssuer(String requiredIssuer) {
    this.requiredIssuer = requiredIssuer;
    return this;
  }
}
