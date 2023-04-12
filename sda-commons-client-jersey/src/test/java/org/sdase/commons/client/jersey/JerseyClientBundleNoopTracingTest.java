package org.sdase.commons.client.jersey;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.MetricFilter;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.client.jersey.test.ClientTestApp;
import org.sdase.commons.client.jersey.test.ClientTestConfig;
import org.sdase.commons.client.jersey.test.MockApiClient;
import org.sdase.commons.client.jersey.wiremock.testing.WireMockClassExtension;

class JerseyClientBundleNoopTracingTest {

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

  @BeforeEach
  void resetRequests() {
    WIRE.resetRequests();
    ClientTestApp app = DW.getApplication();

    // reset the metrics since we don't use it in this test
    DW.getEnvironment().metrics().removeMatching(MetricFilter.ALL);

    WIRE.stubFor(
        get("/api/cars")
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-type", "application/json")
                    .withBody("[]"))); // real value not important

    var cars =
        app.getJerseyClientBundle()
            .getClientFactory()
            .platformClient()
            .api(MockApiClient.class)
            .atTarget(WIRE.baseUrl())
            .getCars();
    assertThat(cars).isNotNull();
  }

  @Test
  void tracerShouldBeNoop() throws ClassNotFoundException {
    OpenTelemetry actual = GlobalOpenTelemetry.get();
    assertThat(actual)
        .extracting("delegate")
        .extracting("propagators")
        .extracting("textMapPropagator")
        .isInstanceOf(Class.forName("io.opentelemetry.context.propagation.NoopTextMapPropagator"));
  }
}
