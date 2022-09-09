package org.sdase.commons.server.opentelemetry;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.servlets.tasks.PostBodyTask;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.opentelemetry.decorators.HeadersUtils;

class OpenTelemetryBundleTest {

  @RegisterExtension
  public static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(TraceTestApp.class, null, randomPorts());

  @RegisterExtension static OpenTelemetryExtension OTEL = OpenTelemetryExtension.create();

  @Test
  void shouldInstrumentServlets() {
    Response r = createClient().path("base/respond/test").request().get();

    List<SpanData> spans = OTEL.getSpans();

    assertThat(r.getStatus()).isEqualTo(SC_OK);
    assertThat(spans).isNotEmpty();
  }

  @Test
  void shouldUsePropagatedContextFromRequestHeaders() {
    String parentId = "ffb768f5b15963f2";
    String traceId = "d38e6f00af20d2ac682ee1fca4fbea01";
    // given very basic W3 format, https://www.w3.org/TR/trace-context/#version-format
    String traceParent = String.format("00-%s-%s-01", traceId, parentId);
    Response r =
        createClient()
            .path("base/respond/traced")
            .request()
            .header("traceparent", traceParent)
            .get();

    assertThat(r.getStatus()).isEqualTo(SC_OK);

    await()
        .untilAsserted(
            () ->
                assertThat(OTEL.getSpans())
                    .isNotEmpty()
                    .hasSize(1)
                    .first()
                    .extracting(SpanData::getParentSpanId)
                    .isEqualTo(parentId));
  }

  @Test
  void shouldChainFiltersInCorrectOrderSoAllSpansAreFinished() {
    for (int i = 0; i < 10; ++i) {
      Response r = createClient().path("base/error").request().get();

      assertThat(r.getStatus()).isEqualTo(SC_INTERNAL_SERVER_ERROR);
    }

    await()
        .untilAsserted(
            () ->
                assertThat(OTEL.getSpans())
                    .as("10 requests should cause 10 root spans")
                    .hasSize(10));
  }

  @Test
  void shouldTraceAndLogExceptions() {
    Response r = createClient().path("base/error").request().get();

    assertThat(r.getStatus()).isEqualTo(SC_INTERNAL_SERVER_ERROR);

    await()
        .untilAsserted(
            () ->
                assertThat(OTEL.getSpans())
                    .isNotEmpty()
                    .hasSize(1)
                    .extracting(SpanData::getStatus)
                    .extracting(StatusData::getStatusCode, StatusData::getDescription)
                    .contains(Tuple.tuple(StatusCode.ERROR, "Something went wrong")));
  }

  @Test
  void shouldDecorateJaxRsSpanWithHeaders() {
    Response r =
        createClient()
            .path("base/respond/test")
            .request()
            .header("Accept", "text/html")
            .header("Authorization", "Bearer eyXXX.yyy.zzz")
            .get();

    assertThat(r.getStatus()).isEqualTo(SC_OK);

    await()
        .untilAsserted(
            () -> {
              List<SpanData> spans = OTEL.getSpans();

              await()
                  .untilAsserted(
                      () ->
                          assertThat(spans)
                              .hasSize(1)
                              .extracting(SpanData::getName)
                              .contains("GET /base/respond/{value}"));
              List<String> spanHeaders =
                  spans.get(0).getAttributes().get(HeadersUtils.HTTP_REQUEST_HEADERS);
              assertThat(spanHeaders)
                  .asList()
                  .doesNotContain("[Authorization = 'Bearer eyXXX.yyy.zzz']")
                  .contains("[Accept = 'text/html']", "[Authorization = 'Bearer ?']");
            });
  }

  @Test
  void shouldSkipConfiguredUrls() {
    Response r = createClient().path("base/respond/skip").request().get();

    assertThat(r.getStatus()).isEqualTo(SC_OK);
    assertThat(OTEL.getSpans()).isEmpty();
  }

  @Test
  void shouldDecorateJaxRsSpansWithResponseHeaders() {
    Response r = createClient().path("base/respond").request().post(null);

    assertThat(r.getStatus()).isEqualTo(SC_CREATED);

    await()
        .untilAsserted(
            () -> {
              List<SpanData> spans = OTEL.getSpans();
              assertThat(OTEL.getSpans())
                  .isNotEmpty()
                  .extracting(SpanData::getName)
                  .contains("POST /base/respond");
              List<String> spanHeaders =
                  spans.get(0).getAttributes().get(HeadersUtils.HTTP_RESPONSE_HEADERS);
              assertThat(spanHeaders)
                  .isNotEmpty()
                  .asList()
                  .contains("[Location = 'http://sdase/id']");
            });
  }

  @Test
  void shouldSkipAdminConfiguredPatterns() {
    Response r = createAdminClient().path("/tasks/skip").request().post(null);
    assertThat(r.getStatus()).isEqualTo(SC_OK);
    assertThat(OTEL.getSpans()).isEmpty();
  }

  @Test
  void shouldTraceAdminTasks() {
    Response someResponse = createAdminClient().path("/tasks/doSomething").request().post(null);
    // no trace for this task
    Response skippedResponse = createAdminClient().path("/tasks/skip").request().post(null);

    assertThat(someResponse.getStatus()).isEqualTo(SC_OK);
    assertThat(skippedResponse.getStatus()).isEqualTo(SC_OK);
    await()
        .untilAsserted(
            () ->
                assertThat(OTEL.getSpans())
                    .hasSize(1)
                    .extracting(SpanData::getName)
                    .contains("POST /tasks/doSomething")
                    .doesNotContain("POST /tasks/skip"));
  }

  private WebTarget createClient() {
    return DW.client().target("http://localhost:" + DW.getLocalPort());
  }

  private WebTarget createAdminClient() {
    return DW.client().target("http://localhost:" + DW.getAdminPort());
  }

  public static class TraceTestApp extends Application<Configuration> {

    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) {
      bootstrap.addBundle(
          OpenTelemetryBundle.builder()
              .withOpenTelemetry(OTEL.getOpenTelemetry())
              .withExcludedUrlsPattern(Pattern.compile("/base/respond/skip|/tasks/skip"))
              .build());
    }

    @Override
    public void run(Configuration configuration, Environment environment) {
      environment.jersey().register(new TestApi());
      environment
          .admin()
          .addTask(
              new PostBodyTask("skip") {
                @Override
                public void execute(
                    Map<String, List<String>> parameters, String body, PrintWriter output) {}
              });
      environment
          .admin()
          .addTask(
              new PostBodyTask("doSomething") {
                @Override
                public void execute(
                    Map<String, List<String>> parameters, String body, PrintWriter output) {}
              });
    }
  }
}
