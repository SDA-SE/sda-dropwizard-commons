package org.sdase.commons.client.jersey.oidc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.MetricFilter;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.Collections;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.sdase.commons.client.jersey.filter.ContainerRequestContextHolder;
import org.sdase.commons.client.jersey.test.ClientTestConfig;
import org.sdase.commons.client.jersey.test.ClientWithoutConsumerTokenTestApp;
import org.sdase.commons.client.jersey.wiremock.testing.WireMockClassExtension;

class OidcRequestFilterTest {

  private static final String CLIENT_ID = "id";
  private static final String CLIENT_SECRET = "secret";
  private static final String GRANT_TYPE = "client_credentials";
  private static final String BEARER_TOKEN = "Bearer xxxx.yyyy.zzzz";
  private static final String BEARER_TOKEN_INCOMING = "Bearer aaaa.bbbb.cccc";

  @RegisterExtension
  @Order(0)
  static final WireMockClassExtension WIRE =
      new WireMockClassExtension(wireMockConfig().dynamicPort());

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
    Response r = app.getClientTestEndPoint().delegate();

    // then
    assertThat(r.getStatus()).isEqualTo(OK.getStatusCode());

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
    Response r = app.getClientTestEndPoint().delegate();

    // then
    assertThat(r.getStatus()).isEqualTo(OK.getStatusCode());

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
    Response r = app.getClientTestEndPoint().delegate();

    // then
    assertThat(r.getStatus()).isEqualTo(OK.getStatusCode());

    verify(1, getRequestedFor(urlEqualTo("/api/cars")).withoutHeader(AUTHORIZATION));
    verify(1, postRequestedFor(urlEqualTo("/token")));
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
          "{\"access_token\": \"xxxx.yyyy.zzzz\",\n"
              + "  \"expires_in\": 60,\n"
              + "  \"token_type\": \"Bearer\"}");
    }

    WIRE.stubFor(
        post("/token")
            .withHeader(
                AUTHORIZATION,
                equalTo(
                    new BasicCredentials(CLIENT_ID, CLIENT_SECRET).asAuthorizationHeaderValue()))
            .withRequestBody(containing("grant_type=" + GRANT_TYPE))
            .willReturn(response));
  }
}
