# SDA Commons Server CORS

The CORS bundle adds a CORS filter to the servlet to allow cross-origin resource sharing for this service.
By doing so, UIs from other origins are allowed to access the service.  

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
      bootstrap.addBundle(CorsBundle.builder().withAuthConfigProvider(MyConfiguration::getCors).build());
      // ...
   }

   @Override
   public void run(MyConfiguration configuration, Environment environment) {
      // ...
   }
}
```


## Configuration
The CORS bundle requires a configuration in your yaml. Otherwise no cors headers will be added to the response
and therefore cross origin resource sharing will fail.

```yaml
cors:
  # List of headers to be added to the allowed headers list
  # Trace and consumer token header as well as the headers "Content-Type, Authorization, X-Requested-With, Accept" are 
  # included within the list by default.
  allowedHeaders: 
    - header1
    - header2
  
  # List of origins that are allowed to use the service. "*" allows all origins
  allowedOrigins:
    - "*"
  # Alternative: If the origins should be restricted, you should add the pattern
  # allowedOrigins:
  #    - https://*.sdase.com
  #    - https://*test.sdase.com
```
