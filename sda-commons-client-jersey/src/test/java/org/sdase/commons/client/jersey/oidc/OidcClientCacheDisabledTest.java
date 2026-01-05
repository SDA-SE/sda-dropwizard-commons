package org.sdase.commons.client.jersey.oidc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
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

class OidcClientCacheDisabledTest {
  private static final String CLIENT_ID = "id";
  private static final String CLIENT_SECRET = "secret";
  private static final String GRANT_TYPE = "client_credentials";

  @RegisterExtension
  @Order(0)
  static final WireMockExtension WIRE = new WireMockExtension.Builder().build();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<ClientTestConfig> DW =
      new DropwizardAppExtension<>(
          ClientTestApp.class,
          resourceFilePath("test-config.yaml"),
          config("oidc.issuerUrl", () -> WIRE.baseUrl() + "/issuer"),
          config("oidc.cache.disabled", "true"));

  private ClientTestApp app;

  @BeforeEach
  void before() {
    WIRE.resetAll();
    app = DW.getApplication();
    app.getOidcClient().clearCache();

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
            .withRequestBody(containing("grant_type=" + GRANT_TYPE))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .withBodyFile("fixtures/tokenResponse.json")));
  }

  @Test
  void shouldCallEndpointsForEachToken() {
    OidcResult firstResult = app.getOidcClient().createAccessToken();
    assertThat(firstResult.getState()).isEqualTo(OidcState.OK);

    OidcResult secondResult = app.getOidcClient().createAccessToken();
    assertThat(secondResult.getState()).isEqualTo(OidcState.OK);

    OidcResult thirdResult = app.getOidcClient().createAccessToken();
    assertThat(thirdResult.getState()).isEqualTo(OidcState.OK);

    // Endpoints should be called for each token
    WIRE.verify(3, getRequestedFor(urlEqualTo("/issuer/.well-known/openid-configuration")));
    WIRE.verify(
        3,
        postRequestedFor(urlEqualTo("/token"))
            .withBasicAuth(new BasicCredentials(CLIENT_ID, CLIENT_SECRET)));
  }
}
