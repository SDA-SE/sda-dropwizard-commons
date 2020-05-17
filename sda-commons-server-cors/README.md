# SDA Commons Server CORS

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-cors/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-cors)

The CORS bundle adds a [CORS](https://www.w3.org/TR/cors/) filter to the servlet to allow cross-origin resource sharing 
for this service. By doing so, UIs from other origins are allowed to access the service.  

## Initialization
To include the CORS filter in an application, the bundle has to be added to the application:

```java
public class MyApplication extends Application<MyConfiguration> {
   
    public static void main(final String[] args) {
        new MyApplication().run(args);
    }

   @Override
   public void initialize(Bootstrap<MyConfiguration> bootstrap) {
      // ...
      bootstrap.addBundle(CorsBundle.builder().withCorsConfigProvider(MyConfiguration::getCors).build());
      // ...
   }

   @Override
   public void run(MyConfiguration configuration, Environment environment) {
      // ...
   }
}
```


## Configuration
The CORS bundle requires an environment specific configuration in your yaml. Otherwise no CORS headers will be 
added to the response and therefore cross origin resource sharing will fail.

```yaml
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

Application specific allowed headers, exposed headers and HTTP methods can be configured in the builder.
