package org.sdase.server.opentelemetry.example;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.util.List;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class OtelTracingApplicationIT {
  @RegisterExtension
  private static final DropwizardAppExtension<Configuration> APP =
      new DropwizardAppExtension<>(OtelTracingApplication.class, new Configuration());

  @RegisterExtension static final OpenTelemetryExtension OTEL = OpenTelemetryExtension.create();

  @BeforeEach
  void setUp() {
    // use InMemory Exporter for testing
    APP.<OtelTracingApplication>getApplication()
        .setTracer(OTEL.getOpenTelemetry().getTracer(OtelTracingApplicationIT.class.getName()));
  }

  @Test
  void shouldGetInstrumented() {
    String response = webTarget("/instrumented").request().get(String.class);
    assertThat(response).isEqualTo("Done!");
    assertThat(OTEL.getSpans())
        .isNotEmpty()
        .hasSize(6)
        .extracting(SpanData::getName)
        // contains 5 nested tasks with span "someWork" and one parent "instrumentedWork
        .containsExactly(
            "someWork", "someWork", "someWork", "someWork", "someWork", "instrumentedWork");
  }

  @Test
  void shouldDoParam() {
    String response = webTarget("/param/foo").request().get(String.class);
    assertThat(response).isEqualTo("foo");
  }

  @Test
  void shouldGetHelloWorld() {
    String response = webTarget("/").request().get(String.class);
    assertThat(response).isEqualTo("Hello World");
  }

  @Test
  void shouldDoError() {
    Response response = webTarget("/error").request().get();
    assertThat(response.getStatus()).isEqualTo(500);
  }

  @Test
  void shouldDoException() {
    Response response = webTarget("/exception").request().get();
    assertThat(response.getStatus()).isEqualTo(200);
    List<SpanData> spans = OTEL.getSpans();
    assertThat(spans)
        .isNotEmpty()
        .hasSize(1)
        .extracting(SpanData::getStatus)
        .extracting(StatusData::getStatusCode, StatusData::getDescription)
        .contains(Tuple.tuple(StatusCode.ERROR, "Something bad happened!"));
  }

  @Test
  void shouldDoRecursive() {
    Response response = webTarget("/recursive").request().get();
    assertThat(response.getStatus()).isEqualTo(200);
  }

  private WebTarget webTarget(String path) {
    return APP.client().target("http://localhost:" + APP.getLocalPort()).path(path);
  }
}
