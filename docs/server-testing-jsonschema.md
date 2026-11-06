# SDA Commons Server Testing: JSON Schema

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-testing/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-testing)

The module `sda-commons-server-testing` provides a
[`JsonSchemaGenerator`](https://github.com/SDA-SE/sda-dropwizard-commons/blob/main/sda-commons-server-testing/src/main/java/org/sdase/commons/server/testing/jsonschema/JsonSchemaGenerator.java)
that generates
[JSON Schema Draft 2020-12](https://json-schema.org/draft/2020-12) documents from Java types.
Use it in tests to generate schemas for payload, request, or response models without maintaining
a separate schema by hand. No additional JSON Schema module is needed.

The default generator reads Jackson, Jakarta Validation, and Swagger annotations from the model.

## Usage

Add [Server Testing](./server-testing.md) to the test dependencies. The
[`sda-commons-bom`](./bom.md) manages its version.

```gradle
dependencies {
  testImplementation 'org.sdase.commons:sda-commons-server-testing'
}
```

### Generate a schema for one type

Create the default generator and pass the root Java type to `generate`:

```java
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.sdase.commons.server.testing.jsonschema.JsonSchemaGenerator;

ObjectNode schema =
    JsonSchemaGenerator.defaultGenerator().generate(CustomerCreated.class);
```

The result contains the Draft 2020-12 `$schema` identifier. Reusable object schemas are stored
under `$defs`.

### Generate a schema catalog for multiple types

Pass an `Iterable` of `Type` instances to generate one catalog. The catalog references every root
type in `oneOf` and stores reusable object schemas under `$defs`. Duplicate roots are ignored. If
only one distinct root remains, the result uses the standalone schema shape without `oneOf`.

```java
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Type;
import java.util.List;
import org.sdase.commons.server.testing.jsonschema.JsonSchemaGenerator;

Type customerCreatedList = new TypeReference<List<CustomerCreated>>() {}.getType();

ObjectNode schemaCatalog =
    JsonSchemaGenerator.defaultGenerator()
        .generate(List.of(CustomerCreated.class, CustomerUpdated.class, customerCreatedList));
```

Use `TypeReference` when a root type contains generic type information that must be retained, such
as `List<CustomerCreated>`.

A consumer can select a specific object schema through its definition reference, for example
`schemas.json#/$defs/CustomerCreated`.

## Document models with annotations

The default generator uses annotations already present on application models:

```java
--8<-- "sda-commons-server-testing/src/test/java/org/sdase/commons/server/testing/jsonschema/CustomerCreated.java"
```

This model produces the following schema:

```json
--8<-- "sda-commons-server-testing/src/test/resources/customer-created-schema.json"
```

The default configuration provides the following behavior:

| Model input                    | JSON Schema behavior                                                                          |
|--------------------------------|-----------------------------------------------------------------------------------------------|
| Jackson annotations            | Apply property names, order, required markers, transformed subtypes, and enum values          |
| Jakarta Validation annotations | Apply supported validation constraints and mark fields with non-null constraints as required  |
| Swagger annotations            | Apply supported schema metadata and constraints, including titles, descriptions, and examples |