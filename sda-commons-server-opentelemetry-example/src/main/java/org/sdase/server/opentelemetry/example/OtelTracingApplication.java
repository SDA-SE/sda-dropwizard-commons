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
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.client.Client;
import org.sdase.commons.client.jersey.JerseyClientBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class OtelTracingApplication extends Application<Configuration> {
  private static final Logger LOGGER = LoggerFactory.getLogger(OtelTracingApplication.class);
  private static final String INSTRUMENTATION_NAME = OtelTracingApplication.class.getName();
  private Tracer tracer;

  private final JerseyClientBundle<Configuration> jerseyClientBundle =
      JerseyClientBundle.builder().build();
  private Client searchClient;
  private Client recursiveClient;

  public static void main(String[] args) throws Exception {
    new OtelTracingApplication().run(args);
  }

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    this.tracer = GlobalOpenTelemetry.getTracer("org.sdase.example-app");
    bootstrap.addBundle(jerseyClientBundle);
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    // it is recommended to only have one instance
    environment.jersey().register(this);

    searchClient =
        jerseyClientBundle.getClientFactory().externalClient().buildGenericClient("search");
    recursiveClient =
        jerseyClientBundle.getClientFactory().platformClient().buildGenericClient("recursive");
    // setup a global Tracer instance
    // It is recommended to create only one instance OpenTelemetry and use it to create
    // all needed tracers in the application.
    OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
    tracer = tracer == null ? openTelemetry.getTracer(INSTRUMENTATION_NAME) : tracer;
  }

  // just for testing
  public OtelTracingApplication setTracer(Tracer tracer) {
    this.tracer = tracer;
    return this;
  }

  @GET
  public String getHelloWorld() {
    // This call has nothing special, but you don't have to do anything to see
    // it in traces.
    return "Hello World";
  }

  @GET
  @Path("search")
  public String doSearch() {
    // This call calls an external service, you can see the GET call in the
    // traces.
    return searchClient.target("http://google.de/").request().get().readEntity(String.class);
  }

  @GET
  @Path("recursive")
  public String doRecursive() {
    // This call calls another service (itself) that is also instrumented with
    // OpenTracing. You can see the GET call and all inner spans inside the
    // called service.
    return recursiveClient
        .target("http://localhost:8080/instrumented")
        .request()
        .get()
        .readEntity(String.class);
  }

  @GET
  @Path("error")
  public String doError() {
    // This call fails with an exceptions. Failed spans are tagged as errors
    // and automatically receive a log entry with the exception.
    throw new InternalServerErrorException("Test");
  }

  @GET
  @Path("param/{test}")
  public String doParam(@PathParam("test") String test) {
    // This call has path parameters. Path parameters are excluded from the
    // operation name, but the full url is still visible inside the tags.
    return test;
  }

  @GET
  @Path("instrumented")
  public String getInstrumented() {
    // This is an example of manual instrumentation:
    // You can create custom spans from the GlobalTracer. A active span has a
    // scope, which is auto closeable. Remember to finish the span after the scope is completed!
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
    // This is an example of manual instrumentation:
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
