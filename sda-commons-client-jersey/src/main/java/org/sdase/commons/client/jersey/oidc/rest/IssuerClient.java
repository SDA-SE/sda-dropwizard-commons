package org.sdase.commons.client.jersey.oidc.rest;

import static org.sdase.commons.client.jersey.proxy.ApiClientInvocationHandler.createProxy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.sdase.commons.client.jersey.ClientFactory;
import org.sdase.commons.client.jersey.oidc.OidcConfiguration;
import org.sdase.commons.client.jersey.oidc.rest.model.OpenIdDiscoveryResource;
import org.sdase.commons.client.jersey.oidc.rest.model.TokenResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IssuerClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(IssuerClient.class);

  private final OidcConfiguration config;
  private final Client client;
  private final OpenIdDiscoveryApi discoveryApi;

  public IssuerClient(ClientFactory clientFactory, OidcConfiguration config) {
    this.config = config;

    this.client =
        clientFactory.externalClient(config.getHttpClient()).buildGenericClient("oidc-client");

    this.discoveryApi =
        createProxy(
            OpenIdDiscoveryApi.class,
            WebResourceFactory.newResource(
                OpenIdDiscoveryApi.class, client.target(config.getIssuerUrl())));
  }

  /**
   * Retrieves a TokenResource from the configured OIDC provider.
   *
   * @return A TokenResource containing the accessToken, tokenType and its expirationTime.
   */
  public TokenResource getTokenResource() {
    OpenIdDiscoveryResource discoveryResource = discoveryApi.getConfiguration();

    if (discoveryResource == null || discoveryResource.getTokenEndpoint() == null) {
      LOGGER.warn("Could not retrieve discovery configuration");
      return null;
    }

    LOGGER.debug("Retrieving access token from {}", discoveryResource.getTokenEndpoint());

    Form tokenForm = createTokenForm();

    final Invocation.Builder requestBuilder =
        client
            .target(discoveryResource.getTokenEndpoint())
            .request(MediaType.APPLICATION_FORM_URLENCODED);
    if (config.isUseAuthHeader()) {
      requestBuilder.header(HttpHeaders.AUTHORIZATION, createBasicAuthHeader());
    }

    TokenResource tokenResource =
        requestBuilder.buildPost(Entity.form(tokenForm)).invoke(TokenResource.class);

    if (tokenResource == null) {
      LOGGER.warn("Could not retrieve access token from {}", discoveryResource.getTokenEndpoint());
    }

    return tokenResource;
  }

  /** Generates the form for the configured grand type. */
  private Form createTokenForm() {
    Form tokenForm = new Form().param("grant_type", config.getGrantType());
    if (!config.isUseAuthHeader()) {
      tokenForm
          .param("client_id", config.getClientId())
          .param("client_secret", config.getClientSecret());
    }
    if ("password".equals(config.getGrantType())) {
      tokenForm.param("username", config.getUsername());
      tokenForm.param("password", config.getPassword());
    }
    return tokenForm;
  }

  /** Creates a basic authentication header. */
  private String createBasicAuthHeader() {
    final String clientIdAndSecret = config.getClientId() + ":" + config.getClientSecret();
    return "Basic "
        + Base64.getEncoder().encodeToString(clientIdAndSecret.getBytes(StandardCharsets.UTF_8));
  }
}
