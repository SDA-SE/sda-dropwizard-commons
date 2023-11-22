package org.sdase.commons.server.dropwizard.bundles.test;

import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Collections;
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
