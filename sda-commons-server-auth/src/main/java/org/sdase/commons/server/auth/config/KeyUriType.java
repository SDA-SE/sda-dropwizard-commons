package org.sdase.commons.server.auth.config;

import org.sdase.commons.server.auth.key.OpenIdProviderDiscoveryKeySource;

public enum KeyUriType {


   /**
    * The Uri of the certificate provides the certificate in PEM format.
    */
   PEM,

   /**
    * The Uri of the certificate defines the root of the
    * <a href="https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfigurationRequest">OpenID Provider</a>
    * To load the certificate the configuration is discovered from
    * {@value OpenIdProviderDiscoveryKeySource#DISCOVERY_PATH} below the Uri. The
    * key itself will be loaded from the {@code jwks_uri}
    */
   OPEN_ID_DISCOVERY,

   /**
    * The Uri of the certificate provides keys as Json in
    * <a href="https://tools.ietf.org/html/draft-ietf-jose-json-web-key-41#section-5">JWKS</a> format
    */
   JWKS;

}
