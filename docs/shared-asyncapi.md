# sda-commons-async-api

!!! warning "Experimental"
Please be aware that SDA SE is likely to change or remove this artifact in the future

This module contains the [`AsyncApiGenerator`](https://github.com/SDA-SE/sda-dropwizard-commons/blob/master/sda-commons-shared-asyncapi/src/main/java/org/sdase/commons/shared/asyncapi/AsyncApiGenerator.java)
to generate [AsyncAPI](https://www.asyncapi.com/) specs from a template and model classes in a
code first approach.
The AsyncAPI specification is the industry standard for defining asynchronous APIs.

## Usage

If the code first approach is used to create an AsyncAPI spec this module provides assistance.
The suggested way to use this module is:

- A template file defining the general `info`rmation, `channels` and `components.messages` using
  the AsyncAPI spec.
  `components.schemas` should be omitted.
- The schema is defined and documented as Java classes in the code as they are used in message
  handlers and consumers.
  Jackson, Jakarta Validation and Swagger 2 annotations can be used for documentation.
- The root classes of messages are referenced in `components.messages.YourMessage.payload.$ref` as
  `class://your.package.MessageModel`.
- The [`AsyncApiGenerator`](https://github.com/SDA-SE/sda-dropwizard-commons/blob/master/sda-commons-shared-asyncapi/src/main/java/org/sdase/commons/shared/asyncapi/AsyncApiGenerator.java)
  is used to combine the template and the generated Json Schema of the models to a self-contained
  spec file.
- The generated AsyncAPI spec is committed into source control.
  This way, the commit history will show intended and unintended changes to the API and the API spec
  is accessible any time without executing any code.
- The API can be view in [AsyncAPI Studio](https://studio.asyncapi.com/).

It is suggested to use it as a test dependency, build the AsyncAPI in a unit test and verify that it
is up-to-date.
The [`GoldenFileAssertions`](https://github.com/SDA-SE/sda-dropwizard-commons/blob/master/sda-commons-server-testing/src/main/java/org/sdase/commons/server/testing/GoldenFileAssertions.java)
from the test module help here.

!!! example "Example: Build AsyncAPI for Cars"
=== "asyncapi_template.yaml"
```yaml
--8<-- "sda-commons-asyncapi/src/test/resources/demo/asyncapi_template.yaml"
```
=== "CarManufactured"
```java
--8<-- "sda-commons-shared-asyncapi/src/test/java/org/sdase/commons/shared/asyncapi/test/data/models/CarManufactured.java"
```
=== "…"
```java
--8<-- "sda-commons-shared-asyncapi/src/test/java/org/sdase/commons/shared/asyncapi/test/data/models/CarScrapped.java"
```
=== "AsyncApiTest"
```java
--8<-- "sda-commons-shared-asyncapi/src/test/java/org/sdase/commons/shared/asyncapi/AsyncApiTest.java"
```
=== "Generated asyncapi.yaml"
```yaml
--8<-- "sda-commons-shared-asyncapi/asyncapi.yaml"
```


### Usage with Existing Schemas

In some cases it is not possible to generate a schema with appropriate documentation, e.g. when a
framework requires to use classes from dependencies that do not contain the expected annotations.

In this case the schema may be added to the template.
This should be used as fallback only, because the schema is not connected to the actual code, it may
diverge over time.

!!! example "Example: Build AsyncAPI with handcrafted schema"
=== "template_with_schema.yaml"
```yaml
--8<-- "sda-commons-shared-asyncapi/src/test/resources/demo/template_with_schema.yaml"
```
=== "Created"
```java
--8<-- "sda-commons-shared-asyncapi/src/test/java/org/sdase/commons/shared/asyncapi/test/data/models/Created.java"
```
=== "ApiWithSchemaTest"
```java
--8<-- "sda-commons-shared-asyncapi/src/test/java/org/sdase/commons/shared/asyncapi/ApiWithSchemaTest.java"
```
=== "Generated asyncapi-schema.yaml"
```yaml
--8<-- "sda-commons-shared-asyncapi/asyncapi-with-schema.yaml"
```


### Generating Schema Files

If desired, the module also allows to generate the JSON schema files, for example to use them to
validate test data.
Please take a look at [`JsonSchemaBuilder`](https://github.com/SDA-SE/sda-dropwizard-commons/blob/master/sda-commons-shared-asyncapi/src/main/java/org/sdase/commons/shared/asyncapi/jsonschema/JsonSchemaBuilder.java)
and it's implementations.
