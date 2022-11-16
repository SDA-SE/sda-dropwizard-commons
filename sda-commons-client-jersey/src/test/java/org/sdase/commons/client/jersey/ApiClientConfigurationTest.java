package org.sdase.commons.client.jersey;

import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.MetricFilter;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.client.jersey.test.ClientTestApp;
import org.sdase.commons.client.jersey.test.ClientTestConfig;
import org.sdase.commons.client.jersey.test.MockApiClient;
import org.sdase.commons.client.jersey.test.MockApiClient.Car;
import org.sdase.commons.client.jersey.wiremock.testing.WireMockClassExtension;

/** Test that http client configuration is correct. */
class ApiClientConfigurationTest {

  @RegisterExtension
  @Order(0)
  private static final WireMockClassExtension WIRE =
      new WireMockClassExtension(wireMockConfig().dynamicPort());

  @RegisterExtension
  @Order(1)
  private static final DropwizardAppExtension<ClientTestConfig> DW =
      new DropwizardAppExtension<>(
          ClientTestApp.class,
          resourceFilePath("test-config.yaml"),
          config("mockBaseUrl", WIRE::baseUrl));

  private ClientTestApp app;

  @BeforeEach
  void setUp() {
    WIRE.resetAll();
    app = DW.getApplication();

    // reset the metrics since we don't use it in this test
    DW.getEnvironment().metrics().removeMatching(MetricFilter.ALL);
  }

  @Test
  void disableGzip() {
    HttpClientConfiguration config = new HttpClientConfiguration();
    config.setGzipEnabled(false);

    MockApiClient client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient(config)
            .api(MockApiClient.class)
            .atTarget(WIRE.baseUrl());

    WIRE.stubFor(
        get("/api/cars") // NOSONAR
            .withHeader("Accept", notMatching("gzip"))
            .willReturn(ok().withHeader("Content-type", "application/json").withBody("[]")));

    assertThat(client.getCars()).isEmpty();
  }

  @Test
  void enableGzipForRequests() {
    HttpClientConfiguration config = new HttpClientConfiguration();
    config.setGzipEnabledForRequests(true);

    MockApiClient client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient(config)
            .api(MockApiClient.class)
            .atTarget(WIRE.baseUrl());

    WIRE.stubFor(
        post("/api/cars") // NOSONAR
            .withHeader("Content-Encoding", matching("gzip"))
            .willReturn(created()));

    assertThat(client.createCar(new Car().setSign("HH UV 42")))
        .extracting(Response::getStatus)
        .isEqualTo(SC_CREATED);
  }

  @Test
  void enableChunkedEncoding() {
    HttpClientConfiguration config = new HttpClientConfiguration();
    config.setChunkedEncodingEnabled(true);

    MockApiClient client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient(config)
            .api(MockApiClient.class)
            .atTarget(WIRE.baseUrl());

    WIRE.stubFor(
        post("/api/cars") // NOSONAR
            .withHeader("Transfer-Encoding", equalTo("chunked"))
            .willReturn(created()));

    assertThat(client.createCar(new Car().setSign("HH UV 42")))
        .extracting(Response::getStatus)
        .isEqualTo(SC_CREATED);
  }
}
