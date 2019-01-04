# SDA Commons Server Starter

The module `sda-commons-server-starter` provides all basics required to build a service for the SDA Platform with
Dropwizard.

Apps built with the [`SdaPlatformBundle`](./src/main/java/org/sdase/commons/server/starter/SdaPlatformBundle.java)
automatically contain

- [Trace Token support](../sda-commons-server-trace/README.md)
- [a tolerant `ObjectMapper`, HAL support and a field filter](../sda-commons-server-jackson/README.md)
- [Security checks on startup](../sda-commons-server-security/README.md)
- [Authentication support](../sda-commons-server-auth/README.md)
- [Prometheus metrics](../sda-commons-server-prometheus/README.md)
- [Swagger documentation](../sda-commons-server-swagger/README.md)

They may be configured easily to

- [require a Consumer token from clients](../sda-commons-server-consumer/README.md)
- [allow cross origin resource sharing](../sda-commons-server-cors/README.md)

Using the [`SdaPlatformBundle`](./src/main/java/org/sdase/commons/server/starter/SdaPlatformBundle.java) is the easiest
and fastest way to create a service for the SDA Platform.

To bootstrap a Dropwizard application for the SDA Platform only the 
[`SdaPlatformBundle`](./src/main/java/org/sdase/commons/server/starter/SdaPlatformBundle.java) has to be added. The 
API should be documented with Swagger annotations: 

```java
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.swagger.annotations.Api;
import org.sdase.commons.server.starter.SdaPlatformBundle;
import org.sdase.commons.server.starter.SdaPlatformConfiguration;

@Api(produces = MediaType.APPLICATION_JSON)
public class MyFirstApp extends Application<SdaPlatformConfiguration> {

   public static void main(String[] args) throws Exception {
      new MyFirstApp().run(args);
   }

   @Override
   public void initialize(Bootstrap<SdaPlatformConfiguration> bootstrap) {
      bootstrap.addBundle(SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            .withSwaggerInfoTitle("My First App")
            // more Swagger data that may also be added with annotations
            .addSwaggerResourcePackageClass(this.getClass())
            .build());
   }

   @Override
   public void run(SdaPlatformConfiguration configuration, Environment environment) {
      environment.jersey().register(MyFirstEndpoint.class);
   }

}


```