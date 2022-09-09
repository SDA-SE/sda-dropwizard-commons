package org.sdase.commons.server.opentelemetry;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import java.net.URI;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/base/")
public class TestApi {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestApi.class);

  @GET
  @Path("error")
  public String doError() {
    throw new InternalServerErrorException("Something went wrong");
  }

  @GET
  @Path("respond/{value}")
  public String doResponse(@PathParam("value") String value) {
    return value;
  }

  @GET
  @Path("log")
  public void doLog() {
    LOGGER.info("Hello World");
  }

  @POST
  @Path("respond")
  public Response doSave() {
    return Response.created(URI.create("http://sdase/id")).build();
  }

  @GET
  @Path("openTracing")
  public String getInstrumented() {
    // This is an example of manual instrumentation:
    // You can create custom spans from the GlobalTracer. A active span has a
    // scope, which is auto closeable. Remember to finish the span after the scope is completed!
    Span span = GlobalTracer.get().buildSpan("openTracing-span").start();

    try (Scope ignored = GlobalTracer.get().scopeManager().activate(span)) {
      span.setTag("legacy-traced-tag", "legacy-traced-tag-value");
    } finally {
      // Don't forget to finish your spans!
      span.finish();
    }

    return "Done!";
  }

  @GET
  @Path("logError")
  public void doLogError() {
    try {
      throw new IllegalStateException("Never call this method");
    } catch (Exception ex) {
      LOGGER.error("Something went wrong", ex);
    }
  }
}
