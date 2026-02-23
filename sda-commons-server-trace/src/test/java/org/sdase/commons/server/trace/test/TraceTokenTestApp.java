package org.sdase.commons.server.trace.test;

import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.trace.TraceTokenBundle;

@Path("/")
public class TraceTokenTestApp extends Application<Configuration> {

  @Override
  public void initialize(Bootstrap bootstrap) {
    bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
    bootstrap.addBundle(TraceTokenBundle.builder().build());
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    environment.jersey().register(this);
    environment.admin().addTask(new TraceTokenAwareExampleTask());
  }

  @OPTIONS
  public Response getOptions() {
    return Response.ok().build();
  }
}
