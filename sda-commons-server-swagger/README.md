# SDA Commons Swagger

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

- as JSON: _<server.rootPath>_/swagger.json
- as YAML: _<server.rootPath>_/swagger.yaml

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
package package org.example.person.api;

//...

@Resource
@ApiModel(description = "Person")
public class PersonResource {

   @ApiModelProperty("The person's first name.")
   private final String firstName;

   @ApiModelProperty("The person's last name.")
   private final String lastName;

   @JsonCreator
   public PersonResource(
         @JsonProperty("firstName") String firstName,
         @JsonProperty("lastName") String lastName) {

      this.firstName = firstName;
      this.lastName = lastName;
   }

   public String getFirstName() {
      return firstName;
   }

   public String getLastName() {
      return lastName;
   }
}
```

The generated documentation would be at:

- as JSON: /api/swagger.json
- as YAML: /api/swagger.yaml
