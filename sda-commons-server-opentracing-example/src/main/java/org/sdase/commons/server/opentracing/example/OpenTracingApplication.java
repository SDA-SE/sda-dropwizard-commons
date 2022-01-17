package org.sdase.commons.server.opentracing.example;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.log.Fields;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.client.Client;
import org.sdase.commons.client.jersey.JerseyClientBundle;
import org.sdase.commons.server.jaeger.JaegerBundle;
import org.sdase.commons.server.opentracing.OpenTracingBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class OpenTracingApplication extends Application<Configuration> {

  static final Logger LOGGER = LoggerFactory.getLogger(OpenTracingApplication.class);

  private final JerseyClientBundle<Configuration> jerseyClientBundle =
      JerseyClientBundle.builder().build();
  private Client searchClient;
  private Client recursiveClient;

  public static void main(String[] args) throws Exception {
    new OpenTracingApplication().run(args);
  }

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    // Initialize Jaeger bundle, make sure to add it before other bundles
    // using OpenTracing instrumentation.
    //
    // Jaeger is a Tracer implementation for OpenTracing that collects traces
    // and sends them to the Jaeger agent. By default the Jaeger agent is
    // expected to run on localhost. In production this is archived by running
    // Jaeger as a sidecar.
    // The bundle can be configured using environment variables, see the
    // README of the JaegerBundle for details.
    bootstrap.addBundle(JaegerBundle.builder().build());
    // Initialize the OpenTracing instrumentation. This makes sure that trace
    // and span ids from upstream services are extracted and initialized. In
    // addition it provides some instrumentation for JAX-RS. The basic
    // instrumentation allows to observe HTTP calls and how they are matched
    // to class methods.
    //
    // You don't have to specify the Jaeger and OpenTracing bundles if you are
    // using the SdaPlatformBundle, in that case the bundles are initialized
    // by default.
    bootstrap.addBundle(OpenTracingBundle.builder().build());
    // The jersey bundle is one of the bundles that are instrumented using
    // OpenTracing. All outgoing HTTP calls are instrumented and trace ids and
    // span ids are also transmitted to downstream services.
    //
    // Other instrumented bundles are the MorphiaBundle and the S3Bundle.
    bootstrap.addBundle(jerseyClientBundle);
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    environment.jersey().register(this);

    searchClient =
        jerseyClientBundle.getClientFactory().externalClient().buildGenericClient("search");
    recursiveClient =
        jerseyClientBundle.getClientFactory().platformClient().buildGenericClient("recursive");
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
    Span span = GlobalTracer.get().buildSpan("instrumentedWork").start();

    try (Scope ignored = GlobalTracer.get().scopeManager().activate(span)) {
      for (int i = 0; i < 5; ++i) {
        runSubTask(i);
      }
    } finally {
      // Don't forget to finish your spans!
      span.finish();
    }

    return "Done!";
  }

  @GET
  @Path("exception")
  public String getException() {
    // This is an example of manual instrumentation:
    Span span = GlobalTracer.get().buildSpan("exceptionWork").start();

    try {
      badMethod();
    } catch (Exception ex) {
      tagException(span, ex); // Let's handle the exception
    } finally {
      // Don't forget to finish your spans!
      span.finish();
    }
    return "Done!";
  }

  private void runSubTask(int index) {
    // This custom span has a tag providing additional knowledge about the
    // span.
    Span span = GlobalTracer.get().buildSpan("someWork").start().setTag("index", index);

    try (Scope ignored = GlobalTracer.get().scopeManager().activate(span)) {
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
      span.finish();
    }
  }

  private void tagException(Span span, Exception ex) {
    // Add exceptions to your span. See the OpenTracing documentation with
    // guidance for common tags and logs:
    // https://github.com/opentracing/specification/blob/master/semantic_conventions.md
    Tags.ERROR.set(span, true);

    Map<String, Object> log = new HashMap<>();
    log.put(Fields.EVENT, "error");
    log.put(Fields.ERROR_OBJECT, ex);
    log.put(Fields.MESSAGE, ex.getMessage());
    span.log(log);
  }

  private void badMethod() {
    throw new IllegalStateException();
  }
}
