package org.sdase.server.opentelemetry.example;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.client.Client;
import org.sdase.commons.client.jersey.JerseyClientBundle;
import org.sdase.commons.server.opentelemetry.OpenTelemetryBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class OpenTelemetryTracingApp extends Application<Configuration> {
  private static final Logger LOGGER = LoggerFactory.getLogger(OpenTelemetryTracingApp.class);
  private static final String INSTRUMENTATION_NAME = OpenTelemetryTracingApp.class.getName();
  private OpenTelemetry openTelemetry;
  private Tracer tracer;

  private final JerseyClientBundle<Configuration> jerseyClientBundle =
      JerseyClientBundle.builder().build();
  private Client recursiveClient;

  public static void main(String[] args) throws Exception {
    new OpenTelemetryTracingApp().run(args);
  }

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    bootstrap.addBundle(jerseyClientBundle);
    openTelemetry = GlobalOpenTelemetry.get();
    bootstrap.addBundle(OpenTelemetryBundle.builder().withTelemetryInstance(openTelemetry).build());
  }

  @Override
  public void run(Configuration configuration, Environment environment) {

    environment.jersey().register(this);
    recursiveClient =
        jerseyClientBundle.getClientFactory().platformClient().buildGenericClient("recursive");
    // get a global telemetry instance
    // The instance should be setup once and registered as global and used everywhere.
    // aquire the globally registered instance
    // create a tracer, the name reflects the lib name and optionally the version(major is enough)
    openTelemetry.getTracer("org.sdase.example-app");
    tracer = tracer == null ? openTelemetry.getTracer(INSTRUMENTATION_NAME) : tracer;
  }

  // only for testing
  public OpenTelemetryTracingApp setTracer(Tracer tracer) {
    this.tracer = tracer;
    return this;
  }

  @GET
  @Path("param/{value}")
  public String doParam(@PathParam("value") String test) {
    // This call should have only one span with name "GET param/{test}"
    return test;
  }

  @GET
  public String getHelloWorld() {
    // this call is not traced
    return "This api is not traced";
  }

  @GET
  @Path("recursive")
  public String doRecursive() {
    // this call s also using other apis that are also instrumented,
    // which means that you should expect a parent span in addition to multiple child spans for the
    // nested calls.
    return recursiveClient
        .target("http://localhost:8080/instrumented")
        .request()
        .get()
        .readEntity(String.class);
  }

  @GET
  @Path("instrumented")
  public String getInstrumented() {
    // This is an example of manual instrumentation:
    // You can create custom span from initialized tracer, make it as current to set the current
    // scope, which is auto closeable. It will be then set automatically as the parent and child
    // spans can be created easily. It is important to set the scope so that child spans are not
    // exported as detached without a parent. would result into:
    // ----instrumentedWork---------------------------------end
    // --someWork---someWork--someWork--someWork--someWork--end
    Span span = tracer.spanBuilder("instrumentedWork").startSpan();

    try (Scope ignored = span.makeCurrent()) {
      for (int i = 0; i < 5; ++i) {
        runSubTask(i);
      }
    } finally {
      // Don't forget to finish your spans!
      span.end();
    }

    return "Done!";
  }

  @GET
  @Path("exception")
  public String getException() {
    // This is an example of manual for handling exceptions. The exception stacktrace will be added
    // to the span and can be seen by any tracing backend.
    // can optionally be set as current
    Span span = tracer.spanBuilder("exceptionWork").startSpan();

    try {
      badMethod();
    } catch (Exception ex) {
      tagException(span, ex); // Let's handle the exception
    } finally {
      // Don't forget to finish your spans!
      span.end();
    }
    return "Done!";
  }

  private void runSubTask(int index) {
    // This custom span has a tag providing additional knowledge about the
    // span.
    Span span = tracer.spanBuilder("someWork").startSpan().setAttribute("index", index);

    try (Scope ignored = span.makeCurrent()) {
      // You can also add log entries to your span
      LOGGER.info("Before sleep");
      Thread.sleep(25);
      LOGGER.info("After sleep");
      if (index % 2 == 0) {
        badMethod();
      }
    } catch (Exception ex) { // NOSONAR Ignore InterruptedException
      tagException(span, ex);
    } finally {
      // Don't forget to finish your spans!
      span.end();
    }
  }

  private void tagException(Span span, Exception ex) {
    // Analogous to OpenTracing log fields that are recommended for instrumentors,
    // OpenTelemetry defines SemanticAttributes that provide similar conventions.
    // https://github.com/open-telemetry/opentelemetry-specification/tree/main/specification/trace/semantic_conventions

    span.setStatus(StatusCode.ERROR, "Something bad happened!");
    span.setAttribute(SemanticAttributes.EXCEPTION_EVENT_NAME, "error");
    span.recordException(ex);
  }

  private void badMethod() {
    throw new IllegalStateException();
  }
}
