package org.sdase.commons.client.jersey;

import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.apache.hc.core5.http.HttpStatus.SC_CREATED;
import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.MetricFilter;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.sdase.commons.client.jersey.test.ClientTestApp;
import org.sdase.commons.client.jersey.test.ClientTestConfig;
import org.sdase.commons.client.jersey.test.MockApiClient;
import org.sdase.commons.client.jersey.test.MockApiClient.Car;
import org.sdase.commons.server.dropwizard.metadata.DetachedMetadataContext;
import org.sdase.commons.server.dropwizard.metadata.MetadataContext;

/** Test that http client configuration is correct. */
@SetSystemProperty(key = "METADATA_FIELDS", value = "tenant-id")
class ApiClientConfigurationTest {

  @RegisterExtension
  @Order(0)
  static final WireMockExtension WIRE =
      new WireMockExtension.Builder().options(wireMockConfig().dynamicPort()).build();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<ClientTestConfig> DW =
      new DropwizardAppExtension<>(
          ClientTestApp.class,
          resourceFilePath("test-config.yaml"),
          config("mockBaseUrl", WIRE::baseUrl));

  private ClientTestApp app;

  @BeforeAll
  static void beforeAll() {
    WireMock.configureFor("http", "localhost", WIRE.getPort());
  }

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

    try (Response response = client.createCar(new Car().setSign("HH UV 42"))) {
      assertThat(response.getStatus()).isEqualTo(SC_CREATED);
    }
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

    try (Response response = client.createCar(new Car().setSign("HH UV 42"))) {
      assertThat(response.getStatus()).isEqualTo(SC_CREATED);
    }
  }

  @Test
  void submitMetadataContextInPlatformClient() {

    MockApiClient client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .platformClient()
            .api(MockApiClient.class)
            .atTarget(WIRE.baseUrl());

    WIRE.stubFor(
        post("/api/cars") // NOSONAR
            .willReturn(created()));
    DetachedMetadataContext metadataContext = new DetachedMetadataContext();
    metadataContext.put("tenant-id", List.of("t-1"));
    MetadataContext.createContext(metadataContext);
    try (var ignored = client.createCar(new Car().setSign("HH UV 42"))) {
      verify(postRequestedFor(urlEqualTo("/api/cars")).withHeader("tenant-id", equalTo("t-1")));
    } finally {
      MetadataContext.createContext(new DetachedMetadataContext());
    }
  }

  @Test
  void dontSubmitMetadataContextInExternalClient() {

    MockApiClient client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient()
            .api(MockApiClient.class)
            .atTarget(WIRE.baseUrl());

    WIRE.stubFor(
        post("/api/cars") // NOSONAR
            .willReturn(created()));
    DetachedMetadataContext metadataContext = new DetachedMetadataContext();
    metadataContext.put("tenant-id", List.of("t-1"));
    MetadataContext.createContext(metadataContext);
    try (var ignored = client.createCar(new Car().setSign("HH UV 42"))) {
      verify(postRequestedFor(urlEqualTo("/api/cars")).withoutHeader("tenant-id"));
    } finally {
      MetadataContext.createContext(new DetachedMetadataContext());
    }
  }
}
