# SDA Commons Shared AsyncAPI

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-shared-asyncapi/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-shared-asyncapi)

> #### ⚠️ Experimental ⚠
>
> Please be aware that this API is in an early stage and might change in the future.
> The JSON schema generator might lack features and could be replaced in the future.
>


This module contains the [`AsyncApiGenerator`](./src/main/java/org/sdase/commons/shared/asyncapi/AsyncApiGenerator.java)
to generate [AsyncAPI](https://www.asyncapi.com/) specs from a template and model classes.
The AsyncAPI specification is the industry standard for defining asynchronous APIs.

## Usage

If the code first approach is used to create an AsyncAPI spec this module provides assistance.
One way to use this module is:

* A template file defining the channels using the AsyncAPI spec is part of the API.
* Definitions for Models classes are generated from code and annotations.
* This module is used to combine the models and template to a self-contained spec file.
* The generated AsyncAPI spec is committed into source control.

A manual written AsyncAPI spec template might look like this and can be stored as a resource:

```yaml
asyncapi: '2.0.0'
id: 'urn:org:sdase:example:cars'
defaultContentType: application/json

info:
  title: Cars Example
  description: This example demonstrates how to define events around *cars*.
  version: '1.0.0'

channels:
  'car-events':
    publish:
      operationId: publishCarEvents
      summary: Car related events
      description: These are all events that are related to a car
      message:
        oneOf:
          - $ref: '#/components/messages/CarManufactured'
          - $ref: '#/components/messages/CarScrapped'

components:
  messages:
    CarManufactured:
      title: Car Manufactured
      description: An event that represents when a new car is manufactured
      payload:
        $ref: './schema.json#/definitions/CarManufactured'
    CarScrapped:
      title: Car Scrapped
      description: An event that represents when a car is scrapped
      payload:
        $ref: './schema.json#/definitions/CarScrapped'
```

To automatically generate the AsyncAPI spec and ensure that it is committed to version control, 
one can use a test like this: 

```java
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;
import org.sdase.commons.server.testing.GoldenFileAssertions;
import org.sdase.commons.shared.asyncapi.AsyncApiGenerator;
import org.sdase.commons.shared.asyncapi.JsonSchemaGenerator;

public class AsyncApiDocumentationTest {
  @Test
  public void generateAndVerifySpec() throws IOException {
    String expected = AsyncApiGenerator
            .builder()
            .withAsyncApiBase(BaseEvent.class.getResource("/asyncapi.yaml"))
            .withSchema("./schema.json", BaseEvent.class)
            .generateYaml();

    // specify where you want your file to be stored
    Path filePath = Paths.get("asyncapi.yaml");

    // check and update the file
    GoldenFileAssertions.assertThat(filePath).hasContentAndUpdateGolden(expected);
  }
}
```

This test uses the [`GoldenFileAssertions` from sda-commons-server-testing](../sda-commons-server-testing).

### Usage with Existing Schemas

If you want to generate a JSON schema with another library or if you have a hand written schema file,
provide a `JsonNode` to `withSchema`:

```java
JsonNode existingSchema = ...
String expected = AsyncApiGenerator
    .builder()
    .withAsyncApiBase(BaseEvent.class.getResource("/asyncapi.yaml"))
    .withSchema("./schema.json", existingSchema)
    .generateYaml();
```


### Generating Schema Files

If desired, the module also allows to generate the JSON schema files, for example to use them to validate test data.
Use [JsonSchemaGenerator](./src/main/java/org/sdase/commons/shared/asyncapi/JsonSchemaGenerator.java) to create standalone JSON schemas:

```java
String expected = JsonSchemaGenerator
    .builder()
    .forClass(BaseEvent.class)
    .withFailOnUnknownProperties(true)
    .generateYaml();
```


## Document Models

You can document the models using annotations like `JsonPropertyDescription` from Jackson or
`JsonSchemaExamples` from [`mbknor-jackson-jsonSchema`](https://github.com/mbknor/mbknor-jackson-jsonSchema).
See the tests of this module for [example model classes](./src/test/java/org/sdase/commons/shared/asyncapi/models).
Note that this requires to add the module as compile time dependency.
