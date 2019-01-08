package org.sdase.commons.server.auth.key;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Loads public keys by discovering the configuration of an OpenID provider.
 */
public class OpenIdProviderDiscoveryKeySource implements KeySource {

   /**
    * The path where the OpenID providers configuration can be discovered from. Further reading:
    * <a href="https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfigurationRequest">OpenID spec 4.1</a>
    */
   public static final String DISCOVERY_PATH = "/.well-known/openid-configuration"; // NOSONAR URL should be configurable but in this case configuration makes no sense

   private String issuerUrl;

   private Client client;

   /**
    * @param issuerUrl the url of the issuer without the {@link #DISCOVERY_PATH}, e.g.
    *                  {@code http://keycloak.example.com/auth/realms/my-realm}
    * @param client the client used to execute the discovery request, may be created from the application
    *               {@link io.dropwizard.setup.Environment} using {@link io.dropwizard.client.JerseyClientBuilder}
    */
   public OpenIdProviderDiscoveryKeySource(String issuerUrl, Client client) {
      this.issuerUrl = issuerUrl;
      this.client = client;
   }

   @Override
   public List<LoadedPublicKey> loadKeysFromSource() {
      try {
         Discovery discovery = client.target(issuerUrl.replaceAll("/$", "") + DISCOVERY_PATH)
               .request(MediaType.APPLICATION_JSON)
               .get(Discovery.class);
         List<LoadedPublicKey> loadedPublicKeys = new JwksKeySource(discovery.getJwksUri(), client).loadKeysFromSource();
         return loadedPublicKeys.stream()
               .map(k -> new LoadedPublicKey(k.getKid(), k.getPublicKey(), this))
               .collect(Collectors.toList());
      }
      catch (KeyLoadFailedException e) {
         throw e;
      }
      catch (Exception e) {
         throw new KeyLoadFailedException(e);
      }
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      OpenIdProviderDiscoveryKeySource that = (OpenIdProviderDiscoveryKeySource) o;
      return Objects.equals(issuerUrl, that.issuerUrl) &&
            Objects.equals(client, that.client);
   }

   @Override
   public int hashCode() {
      return Objects.hash(issuerUrl, client);
   }

   @Override
   public String toString() {
      return "OpenIdProviderDiscoveryKeySource{" +
            "issuerUrl='" + issuerUrl + '\'' +
            '}';
   }

   @JsonIgnoreProperties(ignoreUnknown = true)
   private static class Discovery {

      @JsonProperty("jwks_uri")
      private String jwksUri;

      public String getJwksUri() {
         return jwksUri;
      }

      public Discovery setJwksUri(String jwksUri) {
         this.jwksUri = jwksUri;
         return this;
      }
   }
}
