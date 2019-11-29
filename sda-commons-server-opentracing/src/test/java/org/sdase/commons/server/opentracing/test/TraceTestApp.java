package org.sdase.commons.server.opentracing.test;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.opentracing.mock.MockTracer;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import org.sdase.commons.server.opentracing.OpenTracingBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class TraceTestApp extends Application<Configuration> {
  private static final Logger LOGGER = LoggerFactory.getLogger(TraceTestApp.class);
  private final MockTracer tracer = new MockTracer();

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    bootstrap.addBundle(OpenTracingBundle.builder().withTracer(tracer).build());
  }

  @Override
  public void run(Configuration configuration, Environment environment) throws Exception {
    environment.jersey().register(this);
  }

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

  @GET
  @Path("logError")
  public void doLogError() {
    try {
      throw new IllegalStateException("Never call this method");
    } catch (Exception ex) {
      LOGGER.error("Something went wrong", ex);
    }
  }

  public MockTracer getTracer() {
    return tracer;
  }
}
