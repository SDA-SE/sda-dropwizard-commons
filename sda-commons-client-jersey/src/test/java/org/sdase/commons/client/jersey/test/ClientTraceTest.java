package org.sdase.commons.client.jersey.test;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.http.RequestMethod.GET;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static jakarta.ws.rs.core.HttpHeaders.LOCATION;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import jakarta.ws.rs.client.Client;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ClientTraceTest {

  @RegisterExtension
  @Order(0)
  static final OpenTelemetryExtension OTEL = OpenTelemetryExtension.create();

  @RegisterExtension
  @Order(1)
  static final WireMockExtension WIRE = new WireMockExtension.Builder().build();

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
    assertThat(spans).hasSize(1).first().extracting(SpanData::getName).isEqualTo("GET");

    assertThat(spans.get(0).getAttributes())
        .extracting(
            att -> att.get(AttributeKey.stringKey("url.full")),
            att -> att.get(AttributeKey.stringKey("http.request.method")))
        .contains(WIRE.baseUrl().concat("/"), "GET");
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
