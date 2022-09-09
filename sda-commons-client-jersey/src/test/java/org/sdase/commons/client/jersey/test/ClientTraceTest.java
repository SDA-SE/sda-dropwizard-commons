package org.sdase.commons.client.jersey.test;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.http.RequestMethod.GET;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.List;
import javax.ws.rs.client.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.client.jersey.wiremock.testing.WireMockClassExtension;

class ClientTraceTest {

  @RegisterExtension
  @Order(0)
  static final OpenTelemetryExtension OTEL = OpenTelemetryExtension.create();

  @RegisterExtension
  @Order(1)
  static final WireMockClassExtension WIRE =
      new WireMockClassExtension(wireMockConfig().dynamicPort());

  @RegisterExtension
  @Order(2)
  static final DropwizardAppExtension<ClientTestConfig> DW =
      new DropwizardAppExtension<>(
          ClientTestApp.class,
          resourceFilePath("test-config.yaml"),
          config("mockBaseUrl", WIRE::baseUrl));

  private ClientTestApp app;

  @BeforeEach
  void setUp() {
    WIRE.resetAll();
    WIRE.stubFor(get("/").willReturn(noContent()));
    app = DW.getApplication();
    // removing client metrics to allow creation of new clients with same id
    DW.getEnvironment().metrics().removeMatching((name, metric) -> name.contains(".test."));
  }

  @Test
  void traceClientRequests() {
    Client client =
        app.getJerseyClientBundle().getClientFactory().externalClient().buildGenericClient("test");
    client.target(WIRE.baseUrl()).request().header(LOCATION, "1").header(LOCATION, "2").get();

    List<SpanData> spans = OTEL.getSpans();
    assertThat(spans).hasSize(1).first().extracting(SpanData::getName).isEqualTo("HTTP GET");

    assertThat(spans.get(0).getAttributes())
        .extracting(
            att -> att.get(SemanticAttributes.HTTP_URL),
            att -> att.get(SemanticAttributes.HTTP_METHOD))
        .contains(WIRE.baseUrl(), "GET");
  }

  @Test
  void passTraceIdInHeaders() {
    Client client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient()
            .buildGenericClient("test-traced");
    client.target(WIRE.baseUrl()).request().get();

    WIRE.verify(
        1, newRequestPattern(GET, urlEqualTo("/")).withHeader("traceparent", matching(".+")));
  }
}
