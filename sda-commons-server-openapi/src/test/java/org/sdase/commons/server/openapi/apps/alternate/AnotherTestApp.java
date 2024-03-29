package org.sdase.commons.server.openapi.apps.alternate;

import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.sdase.commons.server.openapi.OpenApiBundle;

@OpenAPIDefinition(info = @Info(title = "Another test app", description = "Test", version = "2"))
public class AnotherTestApp extends Application<Configuration> {

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    bootstrap.addBundle(
        OpenApiBundle.builder().addResourcePackage(getClass().getPackage().getName()).build());
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    // empty
  }
}
