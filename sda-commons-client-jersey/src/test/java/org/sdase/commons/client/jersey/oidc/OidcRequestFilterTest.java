package org.sdase.commons.client.jersey.oidc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.apache.hc.core5.http.HttpHeaders.ACCEPT;
import static org.apache.hc.core5.http.HttpHeaders.CONTENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.MetricFilter;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.sdase.commons.client.jersey.filter.ContainerRequestContextHolder;
import org.sdase.commons.client.jersey.oidc.filter.OidcRequestFilter;
import org.sdase.commons.client.jersey.test.ClientTestConfig;
import org.sdase.commons.client.jersey.test.ClientWithoutConsumerTokenTestApp;

class OidcRequestFilterTest {

  private static final String CLIENT_ID = "id";
  private static final String CLIENT_SECRET = "secret";
  private static final String GRANT_TYPE = "client_credentials";
  private static final String BEARER_TOKEN = "Bearer xxxx.yyyy.zzzz";
  private static final String BEARER_TOKEN_INCOMING = "Bearer aaaa.bbbb.cccc";

  @RegisterExtension
  @Order(0)
  static final WireMockExtension WIRE = new WireMockExtension.Builder().build();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<ClientTestConfig> DW =
      new DropwizardAppExtension<>(
          ClientWithoutConsumerTokenTestApp.class,
          resourceFilePath("test-config.yaml"),
          config("oidc.issuerUrl", () -> WIRE.baseUrl() + "/issuer"),
          config("mockBaseUrl", WIRE::baseUrl));

  private ClientWithoutConsumerTokenTestApp app;
  private static ContainerRequestContext container;

  @BeforeAll
  static void beforeAll() {
    //    Explicitly set the wiremock host and port, to keep configuration after reset in setUp()
    WireMock.configureFor("http", "localhost", WIRE.getPort());
  }

  @BeforeEach
  void before() {
    WIRE.resetAll();
    app = DW.getApplication();
    container = Mockito.mock(ContainerRequestContext.class);
    new ContainerRequestContextHolder().filter(container);

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

    WIRE.stubFor(get("/api/cars").willReturn(aResponse().withStatus(200)));
  }

  @Test
  void shouldAddTokenToRequest() {
    // given
    clearAuthenticationContext();
    setupTokenEndpoint(true);

    // when
    try (Response r = app.getClientTestEndPoint().delegate()) {

      // then
      assertThat(r.getStatus()).isEqualTo(OK.getStatusCode());
    }

    verify(
        1,
        getRequestedFor(urlEqualTo("/api/cars")).withHeader(AUTHORIZATION, equalTo(BEARER_TOKEN)));
    verify(1, postRequestedFor(urlEqualTo("/token")));
  }

  @Test
  void shouldKeepIncomingTokenForRequest() {
    // given
    initializeAuthenticationContext();
    setupTokenEndpoint(true);

    // when
    try (Response r = app.getClientTestEndPoint().delegate()) {

      // then
      assertThat(r.getStatus()).isEqualTo(OK.getStatusCode());
    }

    verify(
        1,
        getRequestedFor(urlEqualTo("/api/cars"))
            .withHeader(AUTHORIZATION, equalTo(BEARER_TOKEN_INCOMING)));
    verify(0, postRequestedFor(urlEqualTo("/token")));
  }

  @Test
  void shouldNotApplyFilterWhenOidcClientBreaks() {
    // given
    clearAuthenticationContext();
    setupTokenEndpoint(false);

    // when
    try (Response r = app.getClientTestEndPoint().delegate()) {

      // then
      assertThat(r.getStatus()).isEqualTo(OK.getStatusCode());
    }

    verify(1, getRequestedFor(urlEqualTo("/api/cars")).withoutHeader(AUTHORIZATION));
    verify(1, postRequestedFor(urlEqualTo("/token")));
  }

  @Test
  void shouldUseCustomNameForMetrics() {
    // given
    clearAuthenticationContext();
    setupTokenEndpoint(true);

    // when new custom filter is used
    var filter =
        new OidcRequestFilter(
            app.getJerseyClientBundle().getClientFactory(),
            DW.getConfiguration().getOidc(),
            true,
            "custom-oidc-client");
    filter.filter(Mockito.mock(ClientRequestContext.class, Mockito.RETURNS_DEEP_STUBS));

    // then metrics show name
    var metrics = DW.getEnvironment().metrics().getMetrics().keySet();
    assertThat(metrics).anyMatch(m -> m.contains(".custom-oidc-client."));
  }

  private void initializeAuthenticationContext() {
    Mockito.when(container.getHeaders())
        .thenReturn(
            new MultivaluedHashMap<>(
                Collections.singletonMap(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_INCOMING)));
  }

  private void clearAuthenticationContext() {
    Mockito.when(container.getHeaders()).thenReturn(new MultivaluedHashMap<>());
  }

  private void setupTokenEndpoint(boolean withBody) {
    ResponseDefinitionBuilder response =
        aResponse().withStatus(OK.getStatusCode()).withHeader(CONTENT_TYPE, APPLICATION_JSON);

    if (withBody) {
      response.withBody(
          """
            {
              "access_token": "xxxx.yyyy.zzzz",
              "expires_in": 60,
              "token_type": "Bearer"
            }
          """);
    }

    WIRE.stubFor(
        post("/token")
            .withHeader(ACCEPT, containing(APPLICATION_FORM_URLENCODED))
            .withHeader(ACCEPT, containing(APPLICATION_JSON))
            .withHeader(
                AUTHORIZATION,
                equalTo(
                    new BasicCredentials(CLIENT_ID, CLIENT_SECRET).asAuthorizationHeaderValue()))
            .withRequestBody(containing("grant_type=" + GRANT_TYPE))
            .willReturn(response));
  }
}
