# SDA Commons Starter

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-starter/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-starter)

The module `sda-commons-starter` provides all basics required to build a service for the SDA Platform with
Dropwizard.

Apps built with the [`SdaPlatformBundle`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-starter/src/main/java/org/sdase/commons/starter/SdaPlatformBundle.java)
automatically contain

- [Support for environment variables in configuration files and default console appender configuration](./server-dropwizard.md)
- [Trace Token support](./server-trace.md)
- [a tolerant `ObjectMapper`, HAL support and a field filter](./server-jackson.md)
- [Security checks on startup](./server-security.md)
- [Authentication support](./server-auth.md)
- [Prometheus metrics](./server-prometheus.md)
- [OpenApi documentation](./server-open-api.md)
- [Open Telemetry](./server-opentelemetry.md)
- [support for the Metadata Context](./server-dropwizard.md#metadatacontextbundle)

They may be configured easily to

- [allow cross-origin resource sharing](./server-cors.md)
- [use the Open Policy Agent for authorization](./server-auth.md)

Using the [`SdaPlatformBundle`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-starter/src/main/java/org/sdase/commons/starter/SdaPlatformBundle.java) is the easiest
and fastest way to create a service for the SDA Platform.

To bootstrap a Dropwizard application for the SDA Platform only the 
[`SdaPlatformBundle`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-starter/src/main/java/org/sdase/commons/starter/SdaPlatformBundle.java) has to be added. The 
API should be documented with Swagger annotations: 

```java
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
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
            // more Open API data that may also be added with annotations
            .addOpenApiResourcePackageClass(this.getClass())
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

The [`SdaPlatformConfiguration`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-starter/src/main/java/org/sdase/commons/starter/SdaPlatformConfiguration.java) may be
extended to add application specific configuration properties.

The `config.yaml` of the 
[`SdaPlatformConfiguration`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-starter/src/main/java/org/sdase/commons/starter/SdaPlatformConfiguration.java) supports
configuration of [authentication](./server-auth.md) and [CORS](./server-cors.md)
additionally to the defaults of Dropwizard's `Configuration`:

```yaml

# See sda-commons-server-auth
auth:
  disableAuth: ${DISABLE_AUTH:-false}
  leeway: ${AUTH_LEEWAY:-0}
  keys: ${AUTH_KEYS:-[]}

opa:
  disableOpa: ${OPA_DISABLE:-false}
  baseUrl: ${OPA_URL:-http://localhost:8181}
  policyPackage: ${OPA_POLICY_PACKAGE:-<your_package>}
  readTimeout: ${OPA_READ_TIMEOUT:-500}

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

# See server-prometheus for other possible configurations.
prometheus:
  # Enables histogram for the `http_server_requests` metric.
  enableRequestHistogram: true
```

By using `.usingSdaPlatformConfiguration()` or `.usingSdaPlatformConfiguration(MyCustomConfiguration.class)` 
the authorization including the open policy agent are automatically enabled as well as the cors settings. 

Instead of `.usingSdaPlatformConfiguration()` and `.usingSdaPlatformConfiguration(MyCustomConfiguration.class)`, the configuration may be fully customized using 
`.usingCustomConfig(MyCustomConfiguration.class)` to support configurations that do not extend 
[`SdaPlatformConfiguration`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-starter/src/main/java/org/sdase/commons/starter/SdaPlatformConfiguration.java). This may 
also be needed to disable some features of the starter module or add special features such as
Authorization.

Please note that `.withOpaAuthorization(MyConfiguration::getAuth, MyConfiguration::getOpa)`
will configure the `AuthBundle` to use `.withExternalAuthorization()`. Please read the 
[documentation of the Auth Bundle](./server-auth.md) carefully before
using this option.
