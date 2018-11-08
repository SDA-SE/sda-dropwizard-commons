# SDA Commons Server Jackson

The module `sda-commons-server-jackson` is used to configure the 
`ObjectMapper` with the recommended default settings of SDA SE services. It also provides support for linking resources 
with HAL and adds the ability to filter fields on client request.

The [`JacksonConfigurationBundle`](./src/main/java/org/sdase/commons/server/jackson/JacksonConfigurationBundle.java) is
used to configure the Json serializer.



## Usage

In the application class, the bundle is added in the `initialize` method:

```java
import JacksonConfigurationBundle;
import io.dropwizard.Application;

public class MyApplication extends Application<MyConfiguration> {
   
    public static void main(final String[] args) {
        new MyApplication().run(args);
    }

   @Override
   public void initialize(Bootstrap<MyConfiguration> bootstrap) {
      // ...
      bootstrap.addBundle(JacksonConfigurationBundle.builder().build());
      // ...
   }

   @Override
   public void run(MyConfiguration configuration, Environment environment) {
      // ...
   }
}
```


### Adding HAL links to resources

Resources that should be processed for HAL links must be annotated as `@Resource`. Links are added directly in the 
resource class and are annotated as `@Link`. Embedded resources can be added as `@EmbeddedResource`. The 
[Open API Tools](https://github.com/openapi-tools/jackson-dataformat-hal) are used to render them in appropriate 
`_links` and `_embedded` properties. Links are properly documented in Swagger when `io.openapitools.hal:swagger-hal` is
added to the dependencies. `io.openapitools.hal:swagger-hal` is shipped with 
[sda-commons-server-swagger](../sda-commons-server-swagger/README.md).

HAL link support may be disabled in the `JacksonConfigurationBundle.builder()`.

Example:

```java
@Resource
public class Person {
   @Link 
   private HALLink self;
   private String name;
   // ...
}
```
```
GET /persons/123

=> {"_links":{"self":{"href":"/persons/123"}},"name":"John Doe"}
```


### Field filtering feature for resources

The `JacksonConfigurationBundle` registers the `FieldFilterModule` to add the `FieldFilterSerializerModifier`. The
modifier uses the JAX-RS request context to identify the query param `fields`. If such a query param is present, only
the requested fields (comma separated) are rendered in the resource view. additionally HAL links and embedded resources
are rendered as well.  

Field filtering support may be disabled in the `JacksonConfigurationBundle.builder()`.

Example:

```java
@EnableFieldFilter
public class Person {
   private String firstName;
   private String surName;
   private String nickName;
   // ...
}
```
```
GET /persons/123?fields=firstName,nickName

=> {"firstName":"John","nickName":"Johnny"}
```



## Configuration

### Disable HAL support

The `JacksonConfigurationBundle` may be initialized without HAL support, if links are not needed or achieved in another
way in the application:

```
JacksonConfigurationBundle.builder().withoutHalSupport().build();
```

### Customize the `ObjectMapper`

Custom configurations of the `ObjectMapper` can be achieved by adding a customization consumer which receives the used
`ObjectMapper` instance:

```
JacksonConfigurationBundle.builder()
    .withCustomization(om -> om.enable(SerializationFeature.INDENT_OUTPUT))
    .build();
```

### Yaml

If the `JacksonYAMLProvider` is available in the classpath, it will be registered to support requests that 
`Accept application/yaml`. This is especially useful for Swagger which provides the `swagger.json` also as 
`swagger.yaml`.

To activate Yaml support, a dependency to `com.fasterxml.jackson.jaxrs:jackson-jaxrs-yaml-provider` has to be added. It
is shipped in an appropriate version with [sda-commons-server-swagger](../sda-commons-server-swagger/README.md).

