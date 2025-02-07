package org.sdase.commons.client.jersey;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.dropwizard.client.proxy.ProxyConfiguration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.util.Duration;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.client.jersey.error.ClientRequestException;
import org.sdase.commons.client.jersey.test.ClientTestApp;
import org.sdase.commons.client.jersey.test.ClientTestConfig;

/**
 * Tests that the client does not send <a href="https://github.com/istio/istio/issues/53239">an
 * upgrade header that breaks ISTIO</a>. This test <em>may</em> be removed and the custom config in
 * the {@code org.sdase.commons.client.jersey.builder.AbstractBaseClientBuilder} may be reverted
 * when the ISTIO issue is solved.
 */
class ClientTlsUpgradeTest {

  @RegisterExtension
  @Order(0)
  static final WireMockExtension WIRE = new WireMockExtension.Builder().build();

  @RegisterExtension
  @Order(0)
  static final WireMockExtension WIRE_TARGET = new WireMockExtension.Builder().build();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<ClientTestConfig> dw =
      new DropwizardAppExtension<>(ClientTestApp.class, resourceFilePath("test-config.yaml"));

  private ClientTestApp app;

  @BeforeEach
  void resetRequests() {
    WIRE.resetRequests();
    app = dw.getApplication();
  }

  @Test
  void shouldNotHaveUpgradeConnectionHeaders() {
    WIRE.stubFor(get("/dummy").willReturn(aResponse().withStatus(200).withBody("")));

    // need a proxy to avoid "Connection: keep-alive" which prevents "Connection: upgrade"
    HttpClientConfiguration clientConfig = new HttpClientConfiguration();
    ProxyConfiguration proxyConfiguration = new ProxyConfiguration();
    proxyConfiguration.setHost("localhost");
    proxyConfiguration.setPort(WIRE.getPort());
    proxyConfiguration.setScheme("http");
    clientConfig.setProxyConfiguration(proxyConfiguration);
    clientConfig.setConnectionTimeout(Duration.milliseconds(50));
    clientConfig.setConnectionRequestTimeout(Duration.milliseconds(50));
    Api client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .platformClient(clientConfig)
            .api(Api.class)
            .atTarget(WIRE.baseUrl());
    try {
      client.get();
    } catch (ClientRequestException e) {
      // this is an expected failure, because the proxy setup is not as it should be
      assertThat(e).hasCauseExactlyInstanceOf(ConnectTimeoutException.class);
    }

    WIRE.verify(
        getRequestedFor(urlPathEqualTo("/dummy"))
            .withoutHeader("Connection") // we don't want Connection: Upgrade
            .withoutHeader("Upgrade") // we don't want Upgrade: TLS/1.2
        );
  }

  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @Path("/")
  public interface Api {

    @GET
    @Path(("/dummy"))
    String get();
  }
}
