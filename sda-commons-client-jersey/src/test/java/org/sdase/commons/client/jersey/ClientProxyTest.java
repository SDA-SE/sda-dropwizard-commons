package org.sdase.commons.client.jersey;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.junitpioneer.jupiter.SetSystemProperty.SetSystemProperties;
import org.sdase.commons.client.jersey.test.ClientTestApp;
import org.sdase.commons.client.jersey.test.ClientTestConfig;
import org.sdase.commons.client.jersey.wiremock.testing.WireMockClassExtension;
import org.sdase.commons.server.testing.SystemPropertyClassExtension;

/** A test that checks if the proxy can be configured via system properties */
@SetSystemProperties({
  @SetSystemProperty(key = "http.proxyHost", value = "0.0.0.0"),
  @SetSystemProperty(key = "http.nonProxyHosts", value = "localhost")
})
class ClientProxyTest {
  @RegisterExtension
  @Order(0)
  private static final WireMockClassExtension CONTENT_WIRE =
      new WireMockClassExtension(wireMockConfig().dynamicPort());

  @RegisterExtension
  @Order(1)
  private static final WireMockClassExtension PROXY_WIRE =
      new WireMockClassExtension(wireMockConfig().dynamicPort());

  @RegisterExtension
  @Order(2)
  private static final SystemPropertyClassExtension PROP =
      new SystemPropertyClassExtension()
          .setProperty("http.proxyPort", () -> "" + PROXY_WIRE.port());

  @RegisterExtension
  @Order(3)
  static final DropwizardAppExtension<ClientTestConfig> DW =
      new DropwizardAppExtension<>(ClientTestApp.class, resourceFilePath("test-config.yaml"));

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
    Response response = getClient("shouldUseProxy").target("http://sda.se").request().get();

    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
    CONTENT_WIRE.verify(0, RequestPatternBuilder.allRequests());
    PROXY_WIRE.verify(
        1,
        RequestPatternBuilder.newRequestPattern(RequestMethod.GET, urlEqualTo("/"))
            .withHeader(HttpHeaders.HOST, equalTo("sda.se")));
  }

  @Test
  void shouldNotUseProxy() {
    String url = format("localhost:%d", CONTENT_WIRE.port());

    // given: expect that the proxy is skipped
    CONTENT_WIRE.stubFor(
        get("/")
            .withHeader(HttpHeaders.HOST, equalTo(url))
            .willReturn(aResponse().withStatus(409)));

    // when
    Response response =
        getClient("shouldNotUseProxy").target(CONTENT_WIRE.baseUrl()).request().get();

    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_CONFLICT);
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
        .externalClient()
        .buildGenericClient(name);
  }
}
