package org.sdase.server.opentelemetry.example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.dropwizard.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class OpenTelemetryTracingAppIT {
  @RegisterExtension
  @Order(1)
  private static final DropwizardAppExtension<Configuration> APP =
      new DropwizardAppExtension<>(OpenTelemetryTracingApp.class, new Configuration());

  @RegisterExtension
  @Order(0)
  private static final OpenTelemetryExtension OTEL = OpenTelemetryExtension.create();

  @Test
  void shouldGetInstrumented() {
    String response = webTarget("/instrumented").request().get(String.class);
    assertThat(response).isEqualTo("Done!");

    await()
        .untilAsserted(
            () ->
                assertThat(OTEL.getSpans())
                    .isNotEmpty()
                    .hasSize(7)
                    .extracting(SpanData::getName)
                    // contains 5 nested tasks with span "someWork" and one parent
                    // "instrumentedWork" and one
                    // parent server "GET /instrumented"
                    .containsExactly(
                        "someWork",
                        "someWork",
                        "someWork",
                        "someWork",
                        "someWork",
                        "instrumentedWork",
                        "GET /instrumented"));

    ;
  }

  @Test
  void shouldGetRoot() {
    String response = webTarget("/").request().get(String.class);
    assertThat(response).isEqualTo("This api is not traced");

    await()
        .untilAsserted(
            () ->
                assertThat(OTEL.getSpans())
                    .isNotEmpty()
                    .extracting(SpanData::getName, s -> s.getInstrumentationScopeInfo().getName())
                    .contains(Tuple.tuple("GET /", "sda-commons.servlet")));
  }

  @Test
  void shouldDoException() {
    Response response = webTarget("/exception").request().get();
    assertThat(response.getStatus()).isEqualTo(200);

    await()
        .untilAsserted(
            () ->
                assertThat(OTEL.getSpans())
                    .isNotEmpty()
                    .hasSize(2)
                    .extracting(SpanData::getStatus)
                    .extracting(StatusData::getStatusCode, StatusData::getDescription)
                    .contains(Tuple.tuple(StatusCode.ERROR, "Something bad happened!")));
    ;
  }

  @Test
  void shouldDoParam() {
    String response = webTarget("/param/foo").request().get(String.class);
    assertThat(response).isEqualTo("foo");
    await()
        .untilAsserted(
            () ->
                assertThat(OTEL.getSpans())
                    .isNotEmpty()
                    .hasSize(1)
                    .extracting(SpanData::getName)
                    .containsExactly("GET /param/{value}"));
  }

  @Test
  void shouldDoRecursive() {
    Response response = webTarget("/recursive").request().get();
    assertThat(response.getStatus()).isEqualTo(200);

    await().untilAsserted(() -> assertThat(OTEL.getSpans()).isNotEmpty().hasSizeGreaterThan(2));
  }

  private WebTarget webTarget(String path) {
    return APP.client().target("http://localhost:" + APP.getLocalPort()).path(path);
  }
}
