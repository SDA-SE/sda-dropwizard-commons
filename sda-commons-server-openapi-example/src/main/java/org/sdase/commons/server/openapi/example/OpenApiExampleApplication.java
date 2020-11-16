package org.sdase.commons.server.openapi.example;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.openapi.example.people.rest.PersonEndpoint;
import org.sdase.commons.starter.SdaPlatformBundle;
import org.sdase.commons.starter.SdaPlatformConfiguration;

/** Example application to show how to document a REST api using swagger for the SDA platform. */
public class OpenApiExampleApplication extends Application<SdaPlatformConfiguration> {

  public static void main(String[] args) throws Exception {
    new OpenApiExampleApplication().run(args);
  }

  @Override
  public void initialize(Bootstrap<SdaPlatformConfiguration> bootstrap) {
    bootstrap.addBundle(
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            // The following part configures the OpenApi bundle. It is required that the resource
            // package
            // class is configured.
            .addOpenApiResourcePackageClass(getClass())
            .build());
  }

  @Override
  public void run(SdaPlatformConfiguration configuration, Environment environment) {
    // build endpoint with it's dependencies
    PersonEndpoint personEndpoint = new PersonEndpoint();

    // register endpoint to map the resources, however take care that swagger might scan your
    // API correctly without adding them here!
    environment.jersey().register(personEndpoint);
  }
}
