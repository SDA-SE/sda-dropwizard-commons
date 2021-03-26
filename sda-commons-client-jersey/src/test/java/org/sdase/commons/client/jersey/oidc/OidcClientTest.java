package org.sdase.commons.client.jersey.oidc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.FixtureHelpers.fixture;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.given;

import com.codahale.metrics.MetricFilter;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.jetty9.JettyHttpServerFactory;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.client.jersey.oidc.model.OidcResult;
import org.sdase.commons.client.jersey.oidc.model.OidcState;
import org.sdase.commons.client.jersey.test.ClientTestApp;
import org.sdase.commons.client.jersey.test.ClientTestConfig;

public class OidcClientTest {

  private static final String CLIENT_ID = "id";
  private static final String CLIENT_SECRET = "secret";
  private static final String GRANT_TYPE = "client_credentials";

  public static final WireMockClassRule WIRE =
      new WireMockClassRule(
          wireMockConfig().dynamicPort().httpServerFactory(new JettyHttpServerFactory()));

  private static final DropwizardAppRule<ClientTestConfig> DW =
      new DropwizardAppRule<>(
          ClientTestApp.class,
          resourceFilePath("test-config.yaml"),
          config("oidc.issuerUrl", () -> WIRE.baseUrl() + "/issuer"));

  @ClassRule public static final RuleChain RULE = RuleChain.outerRule(WIRE).around(DW);

  private ClientTestApp app;

  @Before
  public void before() {
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
            .withHeader(
                AUTHORIZATION,
                equalTo(
                    new BasicCredentials(CLIENT_ID, CLIENT_SECRET).asAuthorizationHeaderValue()))
            .withRequestBody(containing("grant_type=" + GRANT_TYPE))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .withBody(fixture("fixtures/tokenResponse.json"))));
  }

  @Test
  public void shouldRequestConfigurationAndProduceToken() {
    OidcResult firstResult = app.getOidcClient().createAccessToken();
    assertThat(firstResult.getState()).isEqualTo(OidcState.OK);

    // Verify correct format of bearer token
    assertThat(firstResult.getBearerToken()).isEqualTo("Bearer " + firstResult.getAccessToken());
  }

  @Test
  public void shouldUseCacheIfTokensAreRequestedWithinLifetime() {
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
  public void shouldRequestNewTokenAfterCacheExpires() {
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
}
