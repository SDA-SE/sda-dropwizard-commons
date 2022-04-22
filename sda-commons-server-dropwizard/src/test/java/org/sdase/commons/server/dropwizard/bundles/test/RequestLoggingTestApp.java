package org.sdase.commons.server.dropwizard.bundles.test;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.Collections;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.sdase.commons.server.dropwizard.bundles.DefaultLoggingConfigurationBundle;
import org.sdase.commons.server.healthcheck.InternalHealthCheckEndpointBundle;

@Path("test")
@Produces(MediaType.APPLICATION_JSON)
public class RequestLoggingTestApp extends Application<Configuration> {
  private Configuration configuration;

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    bootstrap.addBundle(new DefaultLoggingConfigurationBundle());
    bootstrap.addBundle(InternalHealthCheckEndpointBundle.builder().build());
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    this.configuration = configuration;
    environment.jersey().register(this);
  }

  @GET
  @Path("")
  public Object getTest() {
    return Collections.singletonMap("test", "foo");
  }

  public Configuration getConfiguration() {
    return configuration;
  }
}
