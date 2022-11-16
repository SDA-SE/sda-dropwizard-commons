package org.sdase.commons.client.jersey.oidc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.FixtureHelpers.fixture;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.MetricFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.jetty9.JettyHttpServerFactory;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.client.jersey.oidc.model.OidcResult;
import org.sdase.commons.client.jersey.oidc.model.OidcState;
import org.sdase.commons.client.jersey.test.ClientTestApp;
import org.sdase.commons.client.jersey.test.ClientTestConfig;
import org.sdase.commons.client.jersey.wiremock.testing.WireMockClassExtension;

class OidcClientDisabledTest {

  private static final String CLIENT_ID = "id";
  private static final String CLIENT_SECRET = "secret";

  @RegisterExtension
  @Order(0)
  private static final WireMockClassExtension WIRE =
      new WireMockClassExtension(
          wireMockConfig().dynamicPort().httpServerFactory(new JettyHttpServerFactory()));

  @RegisterExtension
  @Order(1)
  private static final DropwizardAppExtension<ClientTestConfig> DW =
      new DropwizardAppExtension<>(
          ClientTestApp.class,
          resourceFilePath("test-config.yaml"),
          config("oidc.issuerUrl", () -> WIRE.baseUrl() + "/issuer"),
          config("oidc.disabled", "true"));

  private ClientTestApp app;

  @BeforeEach
  void before() throws JsonProcessingException {
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
            .withHeader(
                AUTHORIZATION,
                equalTo(
                    new BasicCredentials(CLIENT_ID, CLIENT_SECRET).asAuthorizationHeaderValue()))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .withBody(fixture("fixtures/tokenResponse.json"))));
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
