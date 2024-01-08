package org.sdase.commons.client.jersey;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.junitpioneer.jupiter.SetSystemProperty.SetSystemProperties;
import org.sdase.commons.client.jersey.test.ClientTestApp;
import org.sdase.commons.client.jersey.test.ClientTestConfig;

/**
 * A test that checks if the proxy can be configured via the configuration class. They should have a
 * higher priority than the system properties.
 */
@SetSystemProperties({
  @SetSystemProperty(key = "http.proxyHost", value = "0.0.0.0"),
  @SetSystemProperty(key = "http.proxyPort", value = "12345"),
  @SetSystemProperty(key = "http.nonProxyHosts", value = "localhost")
})
class ClientConfiguredProxyTest {
  @RegisterExtension
  @Order(0)
  static final WireMockExtension CONTENT_WIRE =
      new WireMockExtension.Builder().build();

  @RegisterExtension
  @Order(1)
  static final WireMockExtension PROXY_WIRE =
      new WireMockExtension.Builder().build();

  @RegisterExtension
  static final DropwizardAppExtension<ClientTestConfig> DW =
      new DropwizardAppExtension<>(
          ClientTestApp.class,
          resourceFilePath("test-config.yaml"),
          config("client.proxy.host", "localhost"),
          config("client.proxy.port", () -> "" + PROXY_WIRE.getPort()),
          config("client.proxy.nonProxyHosts", "localhost"));

  @BeforeEach
  void before() {
    CONTENT_WIRE.resetAll();
    PROXY_WIRE.resetAll();
  }

  @Test
  void shouldUseProxy() {
    // given: expect that the proxy receives the request
    PROXY_WIRE.stubFor(get("/").withHeader(HttpHeaders.HOST, equalTo("sda.se")).willReturn(ok()));

    // when
    try (Response response = getClient("shouldUseProxy").target("http://sda.se").request().get()) {

      // then
      assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
    }
    CONTENT_WIRE.verify(0, RequestPatternBuilder.allRequests());
    PROXY_WIRE.verify(
        1,
        RequestPatternBuilder.newRequestPattern(RequestMethod.GET, urlEqualTo("/"))
            .withHeader(HttpHeaders.HOST, equalTo("sda.se")));
  }

  @Test
  void shouldNotUseProxy() {
    String url = String.format("localhost:%d", CONTENT_WIRE.getPort());

    // given: expect that the proxy is skipped
    CONTENT_WIRE.stubFor(
        get("/")
            .withHeader(HttpHeaders.HOST, equalTo(url))
            .willReturn(aResponse().withStatus(409)));

    // when
    try (Response response =
        getClient("shouldNotUseProxy").target(CONTENT_WIRE.baseUrl()).request().get()) {

      // then
      assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_CONFLICT);
    }
    PROXY_WIRE.verify(0, RequestPatternBuilder.allRequests());
    CONTENT_WIRE.verify(
        1,
        RequestPatternBuilder.newRequestPattern(RequestMethod.GET, urlEqualTo("/"))
            .withHeader(HttpHeaders.HOST, equalTo(url)));
  }

  private Client getClient(String name) {
    return DW.<ClientTestApp>getApplication()
        .getJerseyClientBundle()
        .getClientFactory()
        .externalClient(DW.getConfiguration().getClient())
        .buildGenericClient(name);
  }
}
