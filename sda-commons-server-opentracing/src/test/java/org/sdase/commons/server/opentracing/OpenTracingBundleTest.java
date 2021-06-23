package org.sdase.commons.server.opentracing;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static io.opentracing.log.Fields.ERROR_KIND;
import static io.opentracing.log.Fields.ERROR_OBJECT;
import static io.opentracing.log.Fields.EVENT;
import static io.opentracing.log.Fields.MESSAGE;
import static io.opentracing.log.Fields.STACK;
import static io.opentracing.tag.Tags.COMPONENT;
import static io.opentracing.tag.Tags.HTTP_URL;
import static io.opentracing.tag.Tags.SAMPLING_PRIORITY;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.awaitility.Awaitility.await;
import static org.sdase.commons.server.opentracing.tags.TagUtils.HTTP_REQUEST_HEADERS;
import static org.sdase.commons.server.opentracing.tags.TagUtils.HTTP_RESPONSE_HEADERS;

import io.dropwizard.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockSpan.LogEntry;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.opentracing.test.TraceTestApp;

class OpenTracingBundleTest {

  @RegisterExtension
  public static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(TraceTestApp.class, null, randomPorts());

  private MockTracer tracer;

  @BeforeEach
  void setUp() {
    TraceTestApp app = DW.getApplication();
    tracer = app.getTracer();
    tracer.reset();
  }

  @Test
  void shouldExtractTraceAndSpanIdFromRequestHeaders() {
    Response r =
        createClient()
            .path("respond/mine")
            .request()
            .header("traceid", "1337")
            .header("spanid", "1337")
            .get();

    // Make sure to wait till the request is completed:
    r.readEntity(String.class);

    assertThat(r.getStatus()).isEqualTo(SC_OK);

    await()
        .untilAsserted(
            () ->
                assertThat(tracer.finishedSpans())
                    .flatExtracting(MockSpan::parentId)
                    .contains(1337L));
  }

  @Test
  void shouldInstrumentServlets() {
    Response r = createClient().path("respond/test").request().get();

    // Make sure to wait till the request is completed:
    r.readEntity(String.class);

    assertThat(r.getStatus()).isEqualTo(SC_OK);

    await().untilAsserted(() -> assertThat(collectComponentTags()).contains("java-web-servlet"));
  }

  @Test
  void shouldInstrumentJaxRs() {
    Response r = createClient().path("respond/test").request().get();

    // Make sure to wait till the request is completed:
    r.readEntity(String.class);

    assertThat(r.getStatus()).isEqualTo(SC_OK);

    await().untilAsserted(() -> assertThat(collectComponentTags()).contains("jaxrs"));
  }

  @Test
  void shouldChainFiltersInCorrectOrderSoAllSpansAreFinished() {
    for (int i = 0; i < 10; ++i) {
      Response r = createClient().path("error").request().get();

      // Make sure to wait till the request is completed:
      r.readEntity(String.class);

      assertThat(r.getStatus()).isEqualTo(SC_INTERNAL_SERVER_ERROR);
    }

    await()
        .untilAsserted(
            () ->
                assertThat(tracer.finishedSpans().stream().filter(s -> s.parentId() == 0L))
                    .as("10 requests should cause 10 root spans")
                    .hasSize(10));
  }

  @Test
  void shouldTraceAndLogExceptions() {
    Response r = createClient().path("error").request().get();

    // Make sure to wait till the request is completed:
    r.readEntity(String.class);

    assertThat(r.getStatus()).isEqualTo(SC_INTERNAL_SERVER_ERROR);

    await()
        .untilAsserted(
            () -> {
              MockSpan span =
                  tracer.finishedSpans().stream()
                      .filter(s -> s.operationName().startsWith("GET:"))
                      .findAny()
                      .orElseThrow(IllegalStateException::new);

              assertThat(span.tags()).contains(entry(Tags.ERROR.getKey(), true));
              assertThat(span.logEntries()).flatExtracting(LogEntry::fields).isNotEmpty();
            });
  }

  @Test
  void shouldDecorateJaxRsSpanWithHeaders() {
    Response r = createClient().path("respond/test").request().get();

    // Make sure to wait till the request is completed:
    r.readEntity(String.class);

    assertThat(r.getStatus()).isEqualTo(SC_OK);

    await()
        .untilAsserted(
            () -> {
              MockSpan span =
                  tracer.finishedSpans().stream()
                      .filter(s -> s.operationName().startsWith("GET:"))
                      .findAny()
                      .orElseThrow(IllegalStateException::new);

              Map<String, Object> tags = span.tags();
              assertThat(tags)
                  .contains(
                      entry(COMPONENT.getKey(), "jaxrs"),
                      entry(
                          HTTP_URL.getKey(),
                          "http://localhost:" + DW.getLocalPort() + "/respond/test"),
                      entry(HTTP_RESPONSE_HEADERS.getKey(), "[Content-Type = 'text/html']"));
              assertThat(tags).containsKey(HTTP_REQUEST_HEADERS.getKey());
            });
  }

  @Test
  void shouldDecorateServletSpanWithHeaders() {
    Response r = createClient().path("respond/test").request().get();

    // Make sure to wait till the request is completed:
    r.readEntity(String.class);

    assertThat(r.getStatus()).isEqualTo(SC_OK);

    await()
        .untilAsserted(
            () -> {
              MockSpan span =
                  tracer.finishedSpans().stream()
                      .filter(s -> s.operationName().equals("GET"))
                      .findAny()
                      .orElseThrow(IllegalStateException::new);

              Map<String, Object> tags = span.tags();

              assertThat(tags)
                  .contains(
                      entry(COMPONENT.getKey(), "java-web-servlet"),
                      entry(
                          HTTP_URL.getKey(),
                          "http://localhost:" + DW.getLocalPort() + "/respond/test"));
              assertThat(tags).containsKey(HTTP_REQUEST_HEADERS.getKey());
              assertThat(tags).containsKey(HTTP_RESPONSE_HEADERS.getKey());
            });
  }

  @Test
  void shouldCollectLogStatementsInTrace() {
    Response r = createClient().path("log").request().get();

    // Make sure to wait till the request is completed:
    r.readEntity(String.class);

    assertThat(r.getStatus()).isEqualTo(SC_NO_CONTENT);

    await()
        .untilAsserted(
            () -> {
              MockSpan span =
                  tracer.finishedSpans().stream()
                      .filter(s -> s.operationName().startsWith("GET:"))
                      .findAny()
                      .orElseThrow(IllegalStateException::new);

              Map<String, Object> fields = (Map<String, Object>) span.logEntries().get(0).fields();
              assertThat(fields).containsEntry("level", "INFO");
              assertThat(fields).containsEntry(MESSAGE, "Hello World");
            });
  }

  @Test
  void shouldCollectLogErrorStatementsInTrace() {
    Response r = createClient().path("logError").request().get();

    // Make sure to wait till the request is completed:
    r.readEntity(String.class);

    assertThat(r.getStatus()).isEqualTo(SC_NO_CONTENT);

    await()
        .untilAsserted(
            () -> {
              MockSpan span =
                  tracer.finishedSpans().stream()
                      .filter(s -> s.operationName().startsWith("GET:"))
                      .findAny()
                      .orElseThrow(IllegalStateException::new);
              Map<String, Object> fields = (Map<String, Object>) span.logEntries().get(0).fields();
              assertThat(fields).containsEntry("level", "ERROR");
              assertThat(fields).containsEntry(EVENT, "error");
              assertThat(fields).containsEntry(MESSAGE, "Something went wrong");
              assertThat(fields.get(STACK)).isNotNull();
              assertThat(fields).containsEntry(ERROR_KIND, "java.lang.IllegalStateException");
              assertThat(fields.get(ERROR_OBJECT)).isNotNull();
            });
  }

  @Test
  void shouldInstrumentAdminServletsButAvoidSampling() {
    Response r = createAdminClient().path("healthcheck").request().get();

    // Make sure to wait till the request is completed:
    r.readEntity(String.class);

    assertThat(r.getStatus()).isEqualTo(SC_OK);

    await()
        .untilAsserted(
            () -> {
              MockSpan span =
                  tracer.finishedSpans().stream()
                      .findFirst()
                      .orElseThrow(IllegalStateException::new);
              Map<String, Object> tags = span.tags();

              assertThat(tags).containsEntry(COMPONENT.getKey(), "java-web-servlet");
              assertThat(tags).containsEntry(SAMPLING_PRIORITY.getKey(), 0);
            });
  }

  @Test
  void shouldInstrumentAdminServletsAndSampleIfDebug() {
    Response r =
        createAdminClient().path("healthcheck").request().header("jaeger-debug-id", "test").get();

    // Make sure to wait till the request is completed:
    r.readEntity(String.class);

    assertThat(r.getStatus()).isEqualTo(SC_OK);

    await()
        .untilAsserted(
            () -> {
              MockSpan span =
                  tracer.finishedSpans().stream()
                      .findFirst()
                      .orElseThrow(IllegalStateException::new);
              Map<String, Object> tags = span.tags();

              assertThat(tags).containsEntry(COMPONENT.getKey(), "java-web-servlet");
              assertThat(tags.get(SAMPLING_PRIORITY.getKey())).isNull();
            });
  }

  private WebTarget createClient() {
    return DW.client().target("http://localhost:" + DW.getLocalPort());
  }

  private WebTarget createAdminClient() {
    return DW.client().target("http://localhost:" + DW.getAdminPort());
  }

  private Set<String> collectComponentTags() {
    return tracer.finishedSpans().stream()
        .map(MockSpan::tags)
        .map(tags -> (String) tags.get(COMPONENT.getKey()))
        .collect(Collectors.toSet());
  }
}
