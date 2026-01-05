package org.sdase.commons.client.jersey.oidc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.hc.core5.http.HttpHeaders.ACCEPT;
import static org.apache.hc.core5.http.HttpHeaders.CONTENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.MetricFilter;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.jetty.JettyHttpServerFactory;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.client.jersey.oidc.model.OidcResult;
import org.sdase.commons.client.jersey.oidc.model.OidcState;
import org.sdase.commons.client.jersey.test.ClientTestApp;
import org.sdase.commons.client.jersey.test.ClientTestConfig;

class OidcClientDisabledTest {

  private static final String CLIENT_ID = "id";
  private static final String CLIENT_SECRET = "secret";

  @RegisterExtension
  @Order(0)
  static final WireMockExtension WIRE =
      new WireMockExtension.Builder()
          .options(wireMockConfig().httpServerFactory(new JettyHttpServerFactory()))
          .build();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<ClientTestConfig> DW =
      new DropwizardAppExtension<>(
          ClientTestApp.class,
          resourceFilePath("test-config.yaml"),
          config("oidc.issuerUrl", () -> WIRE.baseUrl() + "/issuer"),
          config("oidc.disabled", "true"));

  private ClientTestApp app;

  @BeforeEach
  void before() {
    WIRE.resetAll();
    app = DW.getApplication();

    // reset the metrics since we don't use it in this test
    DW.getEnvironment().metrics().removeMatching(MetricFilter.ALL);

    WIRE.stubFor(
        get("/issuer/.well-known/openid-configuration")
            .withHeader(ACCEPT, equalTo(APPLICATION_JSON))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .withBody("{ \"token_endpoint\": \"" + WIRE.baseUrl() + "/token\"}")));

    WIRE.stubFor(
        post("/token")
            .withHeader(ACCEPT, containing(APPLICATION_FORM_URLENCODED))
            .withHeader(ACCEPT, containing(APPLICATION_JSON))
            .withHeader(
                AUTHORIZATION,
                equalTo(
                    new BasicCredentials(CLIENT_ID, CLIENT_SECRET).asAuthorizationHeaderValue()))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .withBodyFile("fixtures/tokenResponse.json")));
  }

  @Test
  void shouldSkipTokenCreationWhenDisabled() {
    OidcResult firstResult = app.getOidcClient().createAccessToken();
    assertThat(firstResult.getState()).isEqualTo(OidcState.SKIPPED);

    WIRE.verify(0, getRequestedFor(urlEqualTo("/issuer/.well-known/openid-configuration")));
    WIRE.verify(
        0,
        postRequestedFor(urlEqualTo("/token"))
            .withBasicAuth(new BasicCredentials(CLIENT_ID, CLIENT_SECRET)));
  }
}
