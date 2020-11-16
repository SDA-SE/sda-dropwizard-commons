package org.sdase.commons.starter.example;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.sdase.commons.starter.SdaPlatformBundle;
import org.sdase.commons.starter.SdaPlatformConfiguration;
import org.sdase.commons.starter.example.people.db.PersonManager;
import org.sdase.commons.starter.example.people.rest.PersonEndpoint;

/**
 * Example application to show an easy approach to bootstrap a microservice for the SDA platform.
 */
@OpenAPIDefinition(
    info =
        @Info(
            title = "SDA Platform Example Application",
            description =
                "This application is an example to show a developer how to create a SDA Dropwizard application",
            version = "1.1.1",
            contact =
                @Contact(name = "John Doe", email = "info@example.com", url = "j.doe@example.com")))
public class SdaPlatformExampleApplication extends Application<SdaPlatformConfiguration> {

  public static void main(String[] args) throws Exception {
    new SdaPlatformExampleApplication().run(args);
  }

  @Override
  public void initialize(Bootstrap<SdaPlatformConfiguration> bootstrap) {
    // Add the starter bundle with minimal configuration includes
    //   - Support for environment variables in config files
    //     (from sda-commons-server-dropwizard)
    //   - Adding a trace token to log messages
    //     (from sda-commons-server-trace)
    //   - CORS for Domains as defined in the configuration (may be none)
    //     (from sda-commons-server-cors)
    //   - Tolerant Jackson configuration
    //     (from sda-commons-server-jackson)
    //   - Custom error mappers for SDA compliant error messages
    //     (from sda-commons-server-jackson)
    //   - Support for Json responses with reduced properties by /myResource?fields=id,...
    //     (from sda-commons-server-jackson)
    //   - HAL support
    //     (from sda-commons-server-jackson)
    //   - Security checks at startup
    //     (from sda-commons-server-security)
    //   - Prometheus metrics
    //     (from sda-commons-server-prometheus)
    //   - OpenID Connect authentication (use @PermitAll to allow only access for identified users)
    //     (from sda-commons-server-auth)
    bootstrap.addBundle(
        SdaPlatformBundle.builder()
            // Use all defaults provided by the Server Starter module
            .usingSdaPlatformConfiguration()
            // Require a consumer token from the client. Only swagger.json/yaml is always
            // accessible.
            // (from sda-commons-server-consumer)
            .withRequiredConsumerToken()
            // Additional Swagger documentation properties may be set here until
            // the packages that should be scanned for Swagger documentation annotations are defined
            .addOpenApiResourcePackageClass(this.getClass())
            .build());
  }

  @Override
  public void run(SdaPlatformConfiguration configuration, Environment environment) {
    // build endpoint with it's dependencies
    PersonManager personManager = new PersonManager();
    PersonEndpoint personEndpoint = new PersonEndpoint(personManager);

    // register endpoint to map the resources
    environment.jersey().register(personEndpoint);
  }
}
