# SDA Commons Swagger

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-swagger/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-swagger)

The module `sda-commons-server-swagger` is the base module to add
[Swagger](https://github.com/swagger-api/swagger-core) support for applications in the
SDA SE infrastructure.

## Usage

In the application class, the bundle is added in the `initialize` method:

```java
public class ExampleApp extends Application<Configuration> {

  // ...
  
  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    // ...
    bootstrap.addBundle(
      SwaggerBundle.builder()
        .withTitle(getName())
        .addResourcePackageClass(getClass())
        .build());
    // ...
  }
}
```

The above uses the application's name as the API title, the API version will be `1.0`, and Swagger
will scan resources in the package of the application class.

### Documentation Location
 
The Swagger documentation base path is dependant on DropWizard's [server.rootPath](https://www.dropwizard.io/0.9.1/docs/manual/configuration.html#man-configuration-all):

- as JSON: ```<server.rootPath>/swagger.json``` 
- as YAML: ```<server.rootPath>/swagger.yaml```

### Customizaton Options

The API title:

```java
SwaggerBundle.builder()
    .withTitle("My API")
```

The API version (defaults to `1.0`):

```java
SwaggerBundle.builder()
//...
    .withVersion("1.2")
```

The API description:

```java
SwaggerBundle.builder()
//...
    .withDescription("My description")
```

The API Terms of Service URL:

```java
SwaggerBundle.builder()
//...
    .withTermsOfServiceUrl("https://example.com/terms-of-service")
```

The API contact:

```java
SwaggerBundle.builder()
//...
    .withContact("John Doe", "john.doe@example.com")
```

The API license:

```java
SwaggerBundle.builder()
//...
    .withLicense("Apache License", "https://www.apache.org/licenses/LICENSE-2.0.html")
```

The [packages scanned](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Integration-and-Configuration#configuration-properties)
by Swagger:

```java
SwaggerBundle.builder()
//...
    .addResourcePackageClass(getClass())
    .addResourcePackageClass(Api.class)
    .addResourcePackage("my.package.containing.resources")
```

The `embed` functionality is automatically documented to operations that return embeddable resources. This can be disabled if needed:

```java
SwaggerBundle.builder()
//...
    .disableEmbedParameter()
```

### Note

The customizations above take precedence over the corresponding ones from
[@SwaggerDefinition(@Info)](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X#info)

## Further Information

[Swagger-Core Annotations](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X)

[Best Practices in API Documentation](https://swagger.io/resources/articles/best-practices-in-api-documentation/)
 
## Example
 
_config.yml_ -
[server.rootPath](https://www.dropwizard.io/0.9.1/docs/manual/configuration.html#man-configuration-all)

```yaml
server:
  rootPath: "/api/*"
```
  
_ExampleApp.java_
```java
package package org.example.person.app;

import org.example.person.api.Api;
//...

public class ExampleApp extends Application<Configuration> {

  // ...
  
  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    // ...
    bootstrap.addBundle(
      SwaggerBundle.builder()
        .withTitle("Example Api")
        .addResourcePackageClass(Api.class)
        .withVersion("1.2")
        .withDescription("Example Description")
        .withTermsOfServiceUrl("https://example.com/terms-of-service")
        .withContact("John Doe", "john.doe@example.com")
        .withLicense("Apache License", "https://www.apache.org/licenses/LICENSE-2.0.html")
        .build());
    // ...
  }
}
```

_Api.java_ -
[@SwaggerDefinition](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X#swaggerdefinition)

```java
package package org.example.person.api;

import io.swagger.annotations.SwaggerDefinition;

@SwaggerDefinition
public class Api {}
```

_PersonService.java_ -
[@Api](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X#api),
[@ApiOperation](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X#apioperation),
[@ApiImplicitParams](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X#apiimplicitparam-apiimplicitparams),
[@ApiResponses](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X#apiresponses-apiresponse)

```java
package package org.example.person.api;

//...

@Api
@Path("/persons")
public interface PersonService {

  @GET
  @Path("/john-doe")
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Returns John Doe.", response = PersonResource.class)
  @ApiImplicitParams(
      @ApiImplicitParam(name = "Authorization", required = true, value = "Bearer xxxxxx.yyyyyyy.zzzzzz", dataType = "string", paramType = "header")
  )
  @ApiResponses({
      @ApiResponse(code = 200, message = "Returns John Doe.", response = PersonResource.class),
      @ApiResponse(code = 404, message = "John Doe was not found.")
  })
  PersonResource getJohnDoe();
}
```

_PersonResource.java_ -
[@ApiModel](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X#apimodel),
[@ApiModelProperty](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X#apimodelproperty)

```java
@Resource
@ApiModel(description = "Person")
public class PersonResource {

   @ApiModelProperty("The person's first name.")
   private final String firstName;

   @ApiModelProperty("The person's last name.")
   private final String lastName;
   
   @ApiModelProperty(value = "traits", example = "[\"hipster\", \"generous\"]")
   private final List<String> traits = new ArrayList<>();

   @JsonCreator
   public PersonResource(
         @JsonProperty("firstName") String firstName,
         @JsonProperty("lastName") String lastName,
         @JsonProperty("traits") List<String> traits) {

      this.firstName = firstName;
      this.lastName = lastName;
      this.traits.addAll(traits);
   }

   public String getFirstName() {
      return firstName;
   }

   public String getLastName() {
      return lastName;
   }
   
   public List<String> getTraits() {
      return traits;
   }
}
```

The generated documentation would be at:

- as JSON: ```/api/swagger.json```
- as YAML: ```/api/swagger.yaml```

### Handling example values

The ```SwaggerBundle``` reads example annotations containing complex JSON instead of interpreting
them as String. If this behaviour is undesired, it may be disabled at creation time using the 
builder. If the bundle encounters a value that could be interpreted as JSON, the value is parsed. 
If the value isn't JSON the value is interpreted as a string.
If the example is supplied like ```example = "{\"key\": false}"``` the swagger definition will 
contain the example as ```example: {"key": false}```. 
