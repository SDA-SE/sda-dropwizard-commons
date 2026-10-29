# SDA Commons Shared JSON Schema

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-shared-jsonschema/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-shared-jsonschema)

The module `sda-commons-shared-jsonschema` provides a
[`JsonSchemaGenerator`](https://github.com/SDA-SE/sda-dropwizard-commons/blob/main/sda-commons-shared-jsonschema/src/main/java/org/sdase/commons/shared/jsonschema/JsonSchemaGenerator.java)
that generates
[JSON Schema Draft 2020-12](https://json-schema.org/draft/2020-12) documents from Java types.
Use it to describe payload, request, or response models without maintaining a separate schema by
hand.

The default generator reads Jackson, Jakarta Validation, and Swagger annotations from the model.

## Usage

Add the module to the application or test dependencies. The
[`sda-commons-bom`](./bom.md) manages its version.

```gradle
dependencies {
  implementation 'org.sdase.commons:sda-commons-shared-jsonschema'
}
```

### Generate a schema for one type

Create the default generator and pass the root Java type to `generate`:

```java
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.sdase.commons.shared.jsonschema.JsonSchemaGenerator;

ObjectNode schema =
    JsonSchemaGenerator.defaultGenerator().generate(CustomerCreated.class);
```

The result contains the Draft 2020-12 `$schema` identifier. Reusable object schemas are stored
under `$defs`.

### Generate a schema catalog for multiple types

Pass an `Iterable` of `Type` instances to generate one catalog. The catalog references every root
type in `oneOf` and stores reusable object schemas under `$defs`.

```java
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Type;
import java.util.List;
import org.sdase.commons.shared.jsonschema.JsonSchemaGenerator;

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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Schema(title = "Customer created", description = "Customer creation event")
class CustomerCreated {

  @NotBlank
  @Schema(description = "Customer identifier", example = "customer-42")
  private String customerId;

  @NotNull
  private Instant occurredAt;

  public String getCustomerId() {
    return customerId;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}
```

This model produces these relevant constraints and metadata:

```json
{
  "type": "object",
  "properties": {
    "customerId": {
      "type": "string",
      "minLength": 1,
      "pattern": "^.*\\S+.*$",
      "description": "Customer identifier",
      "examples": ["customer-42"]
    },
    "occurredAt": {
      "type": "string",
      "format": "date-time"
    }
  },
  "required": ["customerId", "occurredAt"],
  "title": "Customer created",
  "description": "Customer creation event"
}
```

The default configuration provides the following behavior:

| Model input                        | JSON Schema behavior                                                                           |
|------------------------------------|------------------------------------------------------------------------------------------------|
| Jackson property annotations       | Apply property names, order, required markers, transformed subtypes, and enum values           |
| Jakarta Validation annotations     | Apply supported constraints and mark fields with non-null constraints as required              |
| `@NotBlank`                        | Add `minLength: 1` and a pattern that requires a non-whitespace character                      |
| Swagger `@Schema`                  | Apply supported schema metadata such as title and description                                  |
| Swagger `@Schema(example = "...")` | Add the value to `examples`; keep strings as text and parse other values as JSON when possible |

## Use a custom Victools configuration

Use the public constructor when the defaults do not fit the schema contract:

```java
SchemaGeneratorConfig config =
    new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
        .build();

JsonSchemaGenerator generator = new JsonSchemaGenerator(config);
```

The supplied
[`SchemaGeneratorConfig`](https://victools.github.io/jsonschema-generator/#generator-configuration)
fully replaces the SDA Commons default configuration. Register every required Victools option and
module in the custom configuration.

