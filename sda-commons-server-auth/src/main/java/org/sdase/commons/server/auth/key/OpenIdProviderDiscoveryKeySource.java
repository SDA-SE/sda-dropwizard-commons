package org.sdase.commons.server.auth.key;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loads public keys by discovering the configuration of an OpenID provider. */
public class OpenIdProviderDiscoveryKeySource implements KeySource {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(OpenIdProviderDiscoveryKeySource.class);

  /**
   * The path where the OpenID providers configuration can be discovered from. Further reading: <a
   * href="https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfigurationRequest">OpenID
   * spec 4.1</a>
   */
  public static final String DISCOVERY_PATH =
      "/.well-known/openid-configuration"; // NOSONAR URL should be configurable but in this case
  // configuration makes no sense

  private final String issuerUrl;

  private final Client client;

  private final String requiredIssuer;
  /**
   * @param issuerUrl the url of the issuer without the {@link #DISCOVERY_PATH}, e.g. {@code
   *     http://keycloak.example.com/auth/realms/my-realm}
   * @param client the client used to execute the discovery request, may be created from the
   *     application {@link io.dropwizard.setup.Environment} using {@link
   *     io.dropwizard.client.JerseyClientBuilder}
   * @param requiredIssuer the required value of the issuer claim of the token in conjunction to the
   *     current key
   */
  public OpenIdProviderDiscoveryKeySource(String issuerUrl, Client client, String requiredIssuer) {
    this.issuerUrl = issuerUrl;
    this.client = client;
    this.requiredIssuer = requiredIssuer;
  }

  @Override
  public List<LoadedPublicKey> loadKeysFromSource() {
    try {
      Discovery discovery =
          client
              .target(issuerUrl.replaceAll("/$", "") + DISCOVERY_PATH)
              .request(MediaType.APPLICATION_JSON)
              .get(Discovery.class);
      List<LoadedPublicKey> loadedPublicKeys =
          new JwksKeySource(discovery.getJwksUri(), client, requiredIssuer).loadKeysFromSource();
      return loadedPublicKeys.stream()
          .map(k -> new LoadedPublicKey(k.getKid(), k.getPublicKey(), this, requiredIssuer))
          .collect(Collectors.toList());
    } catch (KeyLoadFailedException e) {
      throw e;
    } catch (WebApplicationException e) {
      try {
        e.getResponse().close();
      } catch (ProcessingException ex) {
        LOGGER.warn(
            "Error while loading keys from OpenId Provider Discovery while closing response", ex);
      }
      throw new KeyLoadFailedException(e);
    } catch (Exception e) {
      throw new KeyLoadFailedException(e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OpenIdProviderDiscoveryKeySource that = (OpenIdProviderDiscoveryKeySource) o;
    return Objects.equals(issuerUrl, that.issuerUrl) && Objects.equals(client, that.client);
  }

  @Override
  public int hashCode() {
    return Objects.hash(issuerUrl, client);
  }

  @Override
  public String toString() {
    return "OpenIdProviderDiscoveryKeySource{" + "issuerUrl='" + issuerUrl + '\'' + '}';
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
