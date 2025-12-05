package org.sdase.commons.client.jersey.oidc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.hc.core5.http.HttpHeaders.ACCEPT;
import static org.apache.hc.core5.http.HttpHeaders.CONTENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.given;

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

class OidcClientTest {

  private static final String CLIENT_ID = "id";
  private static final String CLIENT_SECRET = "secret";
  private static final String PASSWORD = "test1234";
  private static final String USERNAME = "user1";
  private static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
  private static final String GRANT_TYPE_PASSWORD = "password";

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
          config("oidc.issuerUrl", () -> WIRE.baseUrl() + "/issuer"));

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
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .withBodyFile("fixtures/tokenResponse.json")));
  }

  @Test
  void shouldRequestConfigurationAndProduceToken() {
    OidcConfiguration oidcConfig = DW.getConfiguration().getOidc();
    oidcConfig.setUseAuthHeader(true);

    OidcResult firstResult = app.getOidcClient().createAccessToken();
    assertThat(firstResult.getState()).isEqualTo(OidcState.OK);

    // Verify correct format of bearer token
    assertThat(firstResult.getBearerToken()).isEqualTo("Bearer " + firstResult.getAccessToken());
  }

  @Test
  void shouldUseCacheIfTokensAreRequestedWithinLifetime() {
    OidcConfiguration oidcConfig = DW.getConfiguration().getOidc();
    oidcConfig.setUseAuthHeader(true);

    OidcResult firstResult = app.getOidcClient().createAccessToken();
    assertThat(firstResult.getState()).isEqualTo(OidcState.OK);

    // Should use token from cache for second call
    OidcResult secondResult = app.getOidcClient().createAccessToken();
    assertThat(secondResult.getState()).isEqualTo(OidcState.OK);

    // Since the token is from the cache, the accessToken should be the same
    assertThat(firstResult.getAccessToken()).isEqualTo(secondResult.getAccessToken());

    // Verify correct format of bearer token
    assertThat(firstResult.getBearerToken()).isEqualTo("Bearer " + firstResult.getAccessToken());

    // Endpoints should only be called once, second token should be served through the cache
    WIRE.verify(1, getRequestedFor(urlEqualTo("/issuer/.well-known/openid-configuration")));
    WIRE.verify(
        1,
        postRequestedFor(urlEqualTo("/token"))
            .withBasicAuth(new BasicCredentials(CLIENT_ID, CLIENT_SECRET)));
  }

  @Test
  void shouldRequestNewTokenAfterCacheExpires() {
    OidcConfiguration oidcConfig = DW.getConfiguration().getOidc();
    oidcConfig.setUseAuthHeader(true);

    OidcResult firstResult = app.getOidcClient().createAccessToken();
    assertThat(firstResult.getState()).isEqualTo(OidcState.OK);

    given()
        .pollDelay(5, SECONDS)
        .await()
        .untilAsserted(
            () -> {
              OidcResult secondResult = app.getOidcClient().createAccessToken();
              assertThat(secondResult.getState()).isEqualTo(OidcState.OK);
            });

    // Endpoints should be called twice
    WIRE.verify(2, getRequestedFor(urlEqualTo("/issuer/.well-known/openid-configuration")));
    WIRE.verify(
        2,
        postRequestedFor(urlEqualTo("/token"))
            .withBasicAuth(new BasicCredentials(CLIENT_ID, CLIENT_SECRET)));
  }

  @Test
  void shouldCreateTokenFormForClientCredentialsNotUsingAuthHeader() {
    // given
    OidcConfiguration oidcConfig = DW.getConfiguration().getOidc();

    // Disable auth header so client_id / client_secret go into the form body
    oidcConfig.setUseAuthHeader(false);
    oidcConfig.setGrantType(GRANT_TYPE_CLIENT_CREDENTIALS);
    assertThat(oidcConfig.isUseAuthHeader()).isFalse();

    // when
    OidcResult firstResult = app.getOidcClient().createAccessToken();
    assertThat(firstResult.getState()).isEqualTo(OidcState.OK);

    // then
    WIRE.verify(1, getRequestedFor(urlEqualTo("/issuer/.well-known/openid-configuration")));
    WIRE.verify(
        1,
        postRequestedFor(urlEqualTo("/token"))
            .withoutHeader(AUTHORIZATION)
            .withRequestBody(containing("grant_type=" + GRANT_TYPE_CLIENT_CREDENTIALS))
            .withRequestBody(containing("client_id=" + CLIENT_ID))
            .withRequestBody(containing("client_secret=" + CLIENT_SECRET)));
  }

  @Test
  void shouldCreateTokenFormWithScope() {
    // given
    OidcConfiguration oidcConfig = DW.getConfiguration().getOidc();
    oidcConfig.setUseAuthHeader(true);
    oidcConfig.setScope("email");

    // when
    OidcResult firstResult = app.getOidcClient().createAccessToken();
    assertThat(firstResult.getState()).isEqualTo(OidcState.OK);

    // then
    WIRE.verify(1, getRequestedFor(urlEqualTo("/issuer/.well-known/openid-configuration")));
    WIRE.verify(
        1,
        postRequestedFor(urlEqualTo("/token"))
            .withBasicAuth(new BasicCredentials(CLIENT_ID, CLIENT_SECRET))
            .withRequestBody(containing("scope=email")));
  }

  @Test
  void shouldCreateTokenFormForPasswordGrantType() {
    // given
    OidcConfiguration oidcConfig = DW.getConfiguration().getOidc();
    // configure password grant with body credentials
    oidcConfig.setGrantType(GRANT_TYPE_PASSWORD);
    oidcConfig.setUsername(USERNAME);
    oidcConfig.setPassword(PASSWORD);

    // when
    OidcResult result = app.getOidcClient().createAccessToken();

    // then
    assertThat(result.getState()).isEqualTo(OidcState.OK);
    WIRE.verify(1, getRequestedFor(urlEqualTo("/issuer/.well-known/openid-configuration")));
    WIRE.verify(
        1,
        postRequestedFor(urlEqualTo("/token"))
            .withHeader(
                AUTHORIZATION,
                equalTo(
                    new BasicCredentials(CLIENT_ID, CLIENT_SECRET).asAuthorizationHeaderValue()))
            .withRequestBody(containing("grant_type=" + GRANT_TYPE_PASSWORD))
            .withRequestBody(containing("username=" + USERNAME))
            .withRequestBody(containing("password=" + PASSWORD)));
  }
}
