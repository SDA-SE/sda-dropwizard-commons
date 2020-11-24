# SDA Commons OpenApi

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-openapi/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-openapi)

> #### ⚠️ Experimental ⚠
>
> Please be aware that this API is in an early stage and might change in the future.
>

The module `sda-commons-server-openapi` is the base module to add
[OpenAPI](https://github.com/swagger-api/swagger-core) support for applications in the
SDA infrastructure.
This package produces [OpenApi 3.0 definitions](https://swagger.io/docs/specification/basic-structure/).

## Usage

In the application class, the bundle is added in the `initialize` method:

```java
public class ExampleApp extends Application<Configuration> {

  // ...
  
  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    // ...
    bootstrap.addBundle(
      OpenApiBundle.builder()
        .addResourcePackageClass(getClass())
        .build());
    // ...
  }
}
```

The above will scan resources in the package of the application class.
Customize the OpenAPI definition with the [`OpenAPIDefinition`](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations#OpenAPIDefinition)
on a class in the registered package or use [a configuration file](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Integration-and-Configuration#configuration-file).

### Documentation Location
 
The OpenAPI documentation base path is dependent on DropWizard's [server.rootPath](https://www.dropwizard.io/en/release-2.0.x/manual/configuration.html):

- as JSON: ```<server.rootPath>/openapi.json``` 
- as YAML: ```<server.rootPath>/openapi.yaml```

### Customization Options

The [packages scanned](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Integration-and-Configuration#configuration-properties)
by OpenAPI:

```java
OpenApiBundle.builder()
//...
    .addResourcePackageClass(getClass())
    .addResourcePackageClass(Api.class)
    .addResourcePackage("my.package.containing.resources")
```

## Further Information

[Swagger-Core Annotations](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations)

[Best Practices in API Documentation](https://swagger.io/resources/articles/best-practices-in-api-documentation/)
 
## Example
 
_`config.yml`_ -
[`server.rootPath`](https://www.dropwizard.io/en/release-2.0.x/manual/configuration.html)

```yaml
server:
  rootPath: "/api/*"
```
  
_`ExampleApp.java`_
```java
package org.example.person.app;

import org.example.person.api.Api;
//...

public class ExampleApp extends Application<Configuration> {

  // ...
  
  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    // ...
    bootstrap.addBundle(
      OpenApiBundle.builder()
        .addResourcePackageClass(Api.class)
        .build());
    // ...
  }
}
```

_`Api.java`_ -
[`@OpenAPIDefinition`](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations#OpenAPIDefinition)

```java
package org.example.person.api;

/// ...

@OpenAPIDefinition(
    info =
        @Info(
            title = "Example Api",
            version = "1.2",
            description = "Example Description",
            license = @License(name = "Apache License", url = "https://www.apache.org/licenses/LICENSE-2.0.html"),
            contact = @Contact(name = "John Doe", email = "john.doe@example.com")
        )
)
@SecurityScheme(
    type = SecuritySchemeType.HTTP,
    description = "Passes the Bearer Token (SDA JWT) to the service class.",
    name = "BEARER_TOKEN",
    scheme = "bearer",
    bearerFormat = "JWT")
public class Api {}
```

_PersonService.java_ -
[`@Operation`](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations#operation),
[`@ApiResponse`](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations#apiresponse),
[`@Content`](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations#content),
[`@Schema`](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations#schema)
[`@SecurityRequirement`](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations#securityrequirement)

```java
package org.example.person.api;

//...

@Path("/persons")
public interface PersonService {

  @GET
  @Path("/john-doe")
  @Produces(APPLICATION_JSON)
  @Operation(summary = "Returns John Doe.", security = {@SecurityRequirement(name = "BEARER_TOKEN")})
  @ApiResponse(
      responseCode = "200",
      description = "Returns John Doe.",
      content = @Content(schema = @Schema(implementation = PersonResource.class)))
  @ApiResponse(
      responseCode = "404",
      description = "John Doe was not found.")
  PersonResource getJohnDoe();
}
```

_`PersonResource.java`_ -
[`@Schema`](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations#schema)

```java
@Resource
@Schema(description = "Person")
public class PersonResource {

   @Schema(description = "The person's first name.")
   private final String firstName;

   @Schema(description = "The person's last name.")
   private final String lastName;
   
   @Schema(description = "traits", example = "[\"hipster\", \"generous\"]")
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

- as JSON: ```/api/openapi.json```
- as YAML: ```/api/openapi.yaml```

### Handling example values

The ```OpenApiBundle``` reads example annotations containing complex JSON instead of interpreting
them as String. If the bundle encounters a value that could be interpreted as JSON, the value is parsed. 
If the value isn't JSON the value is interpreted as a string.
If the example is supplied like ```example = "{\"key\": false}"``` the swagger definition will 
contain the example as ```example: {"key": false}```. 

### Use an existing OpenAPI file

When working with the API first approach, it is possible to serve an existing OpenAPI file instead
of generating it using Annotations. It is also possible to combine pre-existing and generated results
into one file.

_[`custom-openapi.yaml`](./src/test/resources/custom-openapi.yaml)_

```yaml
openapi: 3.0.1
info:
  title: A manually written OpenAPI file
  description: This is an example file that was written by hand
  contact:
    email: info@sda.se
  version: '1.1'
paths:
  /house:
    # this path will be added
    put:
      summary: Update a house
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/House'
      responses:
        "201":
          description: The house has been updated
...
```

_`MyApplication.java`_

```java
bootstrap.addBundle(
    OpenApiBundle.builder()
        // optionally configure other resource packages. Note that the values from annotations will
        // override the settings from the imported openapi file.
        .addResourcePackageClass(getClass())
        // provide the path to the existing openapi file (yaml or json) in the classpath
        .withExistingOpenAPIFromClasspathResource("/custom-openapi.yaml")
        .build());
```

> Note: Annotations such as [`@OpenAPIDefinition`](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Annotations#OpenAPIDefinition)
>       will override the contents of the provided OpenAPI file if they are found in a configured
>       resource package. 

