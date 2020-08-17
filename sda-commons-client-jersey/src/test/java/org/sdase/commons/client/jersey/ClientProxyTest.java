package org.sdase.commons.client.jersey;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.client.jersey.test.ClientTestApp;
import org.sdase.commons.client.jersey.test.ClientTestConfig;
import org.sdase.commons.server.testing.LazyRule;
import org.sdase.commons.server.testing.SystemPropertyRule;

/** A test that checks if the proxy can be configured via system properties */
public class ClientProxyTest {
  private static final WireMockClassRule CONTENT_WIRE =
      new WireMockClassRule(wireMockConfig().dynamicPort());

  private static final WireMockClassRule PROXY_WIRE =
      new WireMockClassRule(wireMockConfig().dynamicPort());

  private static final LazyRule<SystemPropertyRule> PROP =
      new LazyRule<>(
          () ->
              new SystemPropertyRule()
                  .setProperty("http.proxyHost", "localhost")
                  .setProperty("http.proxyPort", "" + PROXY_WIRE.port())
                  .setProperty("http.nonProxyHosts", "localhost"));

  private static final DropwizardAppRule<ClientTestConfig> DW =
      new DropwizardAppRule<>(ClientTestApp.class, resourceFilePath("test-config.yaml"));

  @ClassRule
  public static final RuleChain rule =
      RuleChain.outerRule(CONTENT_WIRE).around(PROXY_WIRE).around(PROP).around(DW);

  @Before
  public void before() {
    CONTENT_WIRE.resetAll();
    PROXY_WIRE.resetAll();
  }

  @Test
  public void shouldUseProxy() {
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
  public void shouldNotUseProxy() {
    String url = String.format("localhost:%d", CONTENT_WIRE.port());

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
