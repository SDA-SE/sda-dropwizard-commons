package org.sdase.commons.client.jersey.test;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.http.RequestMethod.GET;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static io.opentracing.tag.Tags.COMPONENT;
import static io.opentracing.tag.Tags.HTTP_URL;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sdase.commons.server.opentracing.tags.TagUtils.HTTP_REQUEST_HEADERS;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import javax.ws.rs.client.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.client.jersey.wiremock.testing.WireMockClassExtension;

class ClientTraceTest {

  @RegisterExtension
  @Order(0)
  private static final WireMockClassExtension WIRE =
      new WireMockClassExtension(wireMockConfig().dynamicPort());

  @RegisterExtension
  @Order(1)
  private static final DropwizardAppExtension<ClientTestConfig> dw =
      new DropwizardAppExtension<>(ClientTestApp.class, resourceFilePath("test-config.yaml"));

  private ClientTestApp app;

  @BeforeEach
  void setUp() {
    WIRE.resetAll();
    WIRE.stubFor(get("/").willReturn(noContent()));
    app = dw.getApplication();
    // removing client metrics to allow creation of new clients with same id
    dw.getEnvironment().metrics().removeMatching((name, metric) -> name.contains(".test."));
  }

  @Test
  void traceClientRequests() {
    Client client =
        app.getJerseyClientBundle().getClientFactory().externalClient().buildGenericClient("test");
    client.target(WIRE.baseUrl()).request().header(LOCATION, "1").header(LOCATION, "2").get();

    MockTracer tracer = app.getTracer();
    assertThat(tracer.finishedSpans()).hasSize(1);
    MockSpan span = tracer.finishedSpans().get(0);
    assertThat(span.operationName()).isEqualTo("GET");
    assertThat(span.tags())
        .contains(
            entry(COMPONENT.getKey(), "jaxrs"),
            entry(HTTP_URL.getKey(), WIRE.baseUrl()),
            entry(HTTP_REQUEST_HEADERS.getKey(), "[Location = '1', '2']"));
  }

  @Test
  void passTraceIdInHeaders() {
    Client client =
        app.getJerseyClientBundle().getClientFactory().externalClient().buildGenericClient("test");
    client.target(WIRE.baseUrl()).request().get();

    WIRE.verify(
        1,
        newRequestPattern(GET, urlEqualTo("/"))
            .withHeader("traceid", matching(".+"))
            .withHeader("spanid", matching(".+")));
  }
}
