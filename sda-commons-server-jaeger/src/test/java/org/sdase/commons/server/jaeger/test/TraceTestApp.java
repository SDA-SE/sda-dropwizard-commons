package org.sdase.commons.server.jaeger.test;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import org.sdase.commons.server.jaeger.JaegerBundle;
import org.sdase.commons.server.opentracing.OpenTracingBundle;
import org.sdase.commons.server.prometheus.PrometheusBundle;

@Path("/")
public class TraceTestApp extends Application<Configuration> {

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    bootstrap.addBundle(PrometheusBundle.builder().build());
    bootstrap.addBundle(JaegerBundle.builder().build());
    bootstrap.addBundle(OpenTracingBundle.builder().build());
  }

  @Override
  public void run(Configuration configuration, Environment environment) throws Exception {
    environment.jersey().register(this);
  }

  @GET
  @Path("simple")
  public String doSimple() {
    return "simple";
  }
}
