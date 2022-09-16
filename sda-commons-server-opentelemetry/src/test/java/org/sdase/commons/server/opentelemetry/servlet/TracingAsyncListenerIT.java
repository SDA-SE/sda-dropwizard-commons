package org.sdase.commons.server.opentelemetry.servlet;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.List;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.opentelemetry.OpenTelemetryBundle;

class TracingAsyncListenerIT {

  @RegisterExtension
  public static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(TracingAsyncListenerIT.TraceTestApp.class, null, randomPorts());

  @RegisterExtension static OpenTelemetryExtension OTEL = OpenTelemetryExtension.create();

  @Test
  void shouldTraceAsyncServlets() {
    Response response = createAdminClient().path("/async/trace").request().get();

    assertThat(response.getStatus()).isEqualTo(SC_OK);

    await()
        .untilAsserted(
            () -> {
              List<SpanData> spans = OTEL.getSpans();
              assertThat(spans)
                  .hasSize(2)
                  .extracting(SpanData::getName)
                  .contains("GET /async/trace", "async-process-test");

              SpanData serverSpan =
                  spans.stream()
                      .filter(s -> s.getName().equals("GET /async/trace"))
                      .findFirst()
                      .orElse(null);
              assertThat(serverSpan).isNotNull();

              SpanData internalSpan =
                  spans.stream()
                      .filter(s -> s.getName().equals("async-process-test"))
                      .findFirst()
                      .orElse(null);
              assertThat(internalSpan).isNotNull();

              // Both spans belong to the same trace
              assertThat(internalSpan.getTraceId()).isEqualTo(serverSpan.getTraceId());
              // The internal must be the child of the server span
              assertThat(internalSpan.getParentSpanId()).isEqualTo(serverSpan.getSpanId());
            });
  }

  @Test
  void shouldCatchErrors() {
    Response response = createAdminClient().path("/async/error").request().get();
    assertThat(response.getStatus()).isEqualTo(SC_INTERNAL_SERVER_ERROR);

    await()
        .untilAsserted(
            () -> {
              List<SpanData> spans = OTEL.getSpans();
              assertThat(spans)
                  .hasSize(1)
                  .extracting(SpanData::getName)
                  .contains("GET /async/error");

              List<EventData> events = spans.get(0).getEvents();
              assertThat(events)
                  .isNotEmpty()
                  .extracting(EventData::getAttributes)
                  .anyMatch(
                      attributes ->
                          StringUtils.contains(
                                  attributes.get(SemanticAttributes.EXCEPTION_TYPE),
                                  "java.io.IOException")
                              && StringUtils.contains(
                                  attributes.get(SemanticAttributes.EXCEPTION_MESSAGE),
                                  "Error while doing something.")
                              && StringUtils.contains(
                                  attributes.get(SemanticAttributes.EXCEPTION_STACKTRACE),
                                  "java.io.IOException: Error while doing something."));
            });
  }

  @Test
  void shouldCatchTimeout() {
    Response response = createAdminClient().path("/async/timeout").request().get();
    assertThat(response.getStatus()).isEqualTo(SC_INTERNAL_SERVER_ERROR);

    await()
        .untilAsserted(
            () ->
                assertThat(OTEL.getSpans())
                    .hasSize(1)
                    .extracting(
                        SpanData::getName,
                        spanData ->
                            spanData
                                .getAttributes()
                                .get(TracingAsyncListener.REQUEST_TIMEOUT_ATTRIBUTE_KEY))
                    .contains(tuple("GET /async/timeout", 100L)));
  }

  private WebTarget createAdminClient() {
    return DW.client().target("http://localhost:" + DW.getAdminPort());
  }

  public static class TraceTestApp extends Application<Configuration> {

    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) {
      bootstrap.addBundle(
          OpenTelemetryBundle.builder().withOpenTelemetry(OTEL.getOpenTelemetry()).build());
    }

    @Override
    public void run(Configuration configuration, Environment environment) {
      environment
          .getAdminContext()
          .addServlet(TestAsyncServlet.class, "/async/*")
          .setAsyncSupported(true);
    }
  }
}
