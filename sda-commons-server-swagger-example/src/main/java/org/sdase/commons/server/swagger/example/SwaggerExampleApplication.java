package org.sdase.commons.server.swagger.example;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.starter.SdaPlatformBundle;
import org.sdase.commons.server.starter.SdaPlatformConfiguration;
import org.sdase.commons.server.swagger.example.people.rest.PersonEndpoint;

/** Example application to show how to document a REST api using swagger for the SDA platform. */
public class SwaggerExampleApplication extends Application<SdaPlatformConfiguration> {

  public static void main(String[] args) throws Exception {
    new SwaggerExampleApplication().run(args);
  }

  @Override
  public void initialize(Bootstrap<SdaPlatformConfiguration> bootstrap) {
    bootstrap.addBundle(
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            // The following part configures the info section of the swagger documentation.
            // Only the title as well as the resource package class needs to be configured here.
            // The other values are optional and might also be set using annotations, more details
            // at
            // https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X#swaggerdefinition
            //
            // You don't have to set the base path of your REST API as it's automatically derived
            // while generating the documentation.
            //
            // Set the name of the service as title of the Swagger documentation.
            .withSwaggerInfoTitle("Swagger Example Application")
            // Set the description of the API, like an introduction. Like in most of the fields you
            // may use markdown here to apply custom formatting.
            .withSwaggerInfoDescription(
                "This is the API documentation for the **People API**.\n\n"
                    + "The API provides operations for managing and searching people.")
            // Set the version of the API, which is a string. By default version 1 is used.
            .withSwaggerInfoVersion("3")
            // Set the contact information of the API author.
            .withSwaggerInfoContact("SDA SE", "info@example.com", "https://myfuture.sda-se.com")
            // Set the name of the license of the API as well as an url to the full license
            // information.
            // Not the license is only an example, this code or the api isn't licensed under Apache
            // 2.0.
            .withSwaggerInfoLicense("Apache 2.0", "http://www.apache.org/licenses/LICENSE-2.0.html")
            // For APIs with public availability, terms of service might be required.
            // Set the url to the terms of service here.
            .withSwaggerInfoTermsOfServiceUrl("https://www.sda-se.com/legal-notice/")
            // Set the packages that should be scanned for Swagger documentation annotations.
            .addSwaggerResourcePackageClass(this.getClass())
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
