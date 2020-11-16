# SDA Commons Starter

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-starter/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-starter)

The module `sda-commons-starter` provides all basics required to build a service for the SDA Platform with
Dropwizard.

Apps built with the [`SdaPlatformBundle`](./src/main/java/org/sdase/commons/starter/SdaPlatformBundle.java)
automatically contain

- [Support for environment variables in configuration files and default console appender configuration](../sda-commons-server-dropwizard/README.md)
- [Trace Token support](../sda-commons-server-trace/README.md)
- [a tolerant `ObjectMapper`, HAL support and a field filter](../sda-commons-server-jackson/README.md)
- [Security checks on startup](../sda-commons-server-security/README.md)
- [Authentication support](../sda-commons-server-auth/README.md)
- [Prometheus metrics](../sda-commons-server-prometheus/README.md)
- [OpenApi documentation](../sda-commons-server-openapi/README.md)
- [Open Tracing](../sda-commons-server-opentracing/README.md) and [Jaeger](../sda-commons-server-jaeger/README.md)

They may be configured easily to

- [require a Consumer token from clients](../sda-commons-server-consumer/README.md)
- [allow cross origin resource sharing](../sda-commons-server-cors/README.md)
- [use the Open Policy Agent for authorization](../sda-commons-server-auth/README.md)

Using the [`SdaPlatformBundle`](./src/main/java/org/sdase/commons/starter/SdaPlatformBundle.java) is the easiest
and fastest way to create a service for the SDA Platform.

To bootstrap a Dropwizard application for the SDA Platform only the 
[`SdaPlatformBundle`](./src/main/java/org/sdase/commons/starter/SdaPlatformBundle.java) has to be added. The 
API should be documented with Swagger annotations: 

```java
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import SdaPlatformBundle;
import SdaPlatformConfiguration;

@OpenAPIDefinition(info = @Info(
    title = "My First App",
    description =
        "The description of my first application",
    version = "1.0.0",
    contact =
    @Contact(
        name = "John Doe",
        email = "info@example.com",
        url = "j.doe@example.com"),
    license = @License(name = "Sample License")))
public class MyFirstApp extends Application<SdaPlatformConfiguration> {

   public static void main(String[] args) throws Exception {
      new MyFirstApp().run(args);
   }

   @Override
   public void initialize(Bootstrap<SdaPlatformConfiguration> bootstrap) {
      bootstrap.addBundle(SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            // more Swagger data that may also be added with annotations
            .addSwaggerResourcePackageClass(this.getClass())
            // or use an existing OpenApi definition
            .withExistingOpenAPI("openApiJsonOrYaml")
            .build());
   }

   @Override
   public void run(SdaPlatformConfiguration configuration, Environment environment) {
      environment.jersey().register(MyFirstEndpoint.class);
   }

}
```

The [`SdaPlatformConfiguration`](./src/main/java/org/sdase/commons/starter/SdaPlatformConfiguration.java) may be
extended to add application specific configuration properties.

The `config.yaml` of the 
[`SdaPlatformConfiguration`](./src/main/java/org/sdase/commons/starter/SdaPlatformConfiguration.java) supports
configuration of [authentication](../sda-commons-server-auth/README.md) and [CORS](../sda-commons-server-cors/README.md)
additionally to the defaults of Dropwizard's `Configuration`:

```yaml

# See sda-commons-server-auth
auth:
  disableAuth: ${DISABLE_AUTH:-false}
  leeway: ${AUTH_LEEWAY:-0}
  keys: ${AUTH_KEYS:-[]}

# See sda-commons-server-cors
cors:
  # List of origins that are allowed to use the service. "*" allows all origins
  allowedOrigins:
    - "*"
  # Alternative: If the origins should be restricted, you should add the pattern
  # allowedOrigins:
  #    - https://*.sdase.com
  #    - https://*test.sdase.com
  # To use configurable patterns per environment the Json in Yaml syntax may be used with an environment placeholder:
  # allowedOrigins: ${CORS_ALLOWED_ORIGINS:-["*"]}
```

Instead of `.usingSdaPlatformConfiguration()`, the configuration may be fully customized using 
`.usingCustomConfig(MyCustomConfiguration.class)` to support configurations that do not extend 
[`SdaPlatformConfiguration`](./src/main/java/org/sdase/commons/starter/SdaPlatformConfiguration.java). This may 
also be needed to disable some features of the starter module or add special features such as
Authorization.

Please note that `.withOpaAuthorization(MyConfiguration::getAuth, MyConfiguration::getOpa)`
will configure the `AuthBundle` to use `.withExternalAuthorization()`. Please read the 
[documentation of the Auth Bundle](../sda-commons-server-auth/README.md) carefully before
using this option.
