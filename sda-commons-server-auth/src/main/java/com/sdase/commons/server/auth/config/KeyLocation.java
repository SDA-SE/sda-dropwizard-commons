package com.sdase.commons.server.auth.config;

import java.net.URI;

/**
 * Defines a location of keys.
 */
public class KeyLocation {

   /**
    * <p>Uri leading to</p>
    * <ul>
    *    <li>a JSON Web Key Set, </li>
    *    <li>a OpenID provider base Uri or</li>
    *    <li>a key file in PEM format.</li>
    * </ul>
    * <p>
    *    The type of the Uri depends on the {@link #type}.
    * </p>
    * <p>
    *    Further information:
    * </p>
    * <ul>
    *    <li><a href="https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata">jwks_uri in OpenID spec</a></li>
    *    <li><a href="https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfigurationRequest">OpenID Provider</a></li>
    * </ul>
    */
   private URI location;

   /**
    * The type of the {@link #location} that defines how the certificate is loaded.
    */
   private KeyUriType type;

   /**
    * Optional <a href="https://tools.ietf.org/html/draft-ietf-jose-json-web-key-41#section-4.5">Key Id</a> used only if
    * the {@link #type} is {@link KeyUriType#PEM}.
    */
   private String pemKeyId;

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
}
