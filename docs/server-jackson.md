# SDA Commons Server Jackson

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-jackson/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-jackson)

The module `sda-commons-server-jackson` is used to configure the `ObjectMapper` with the recommended default settings 
of SDA SE services. It also provides support for linking resources with HAL and adds the ability to filter fields on 
client request.

The [`JacksonConfigurationBundle`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-server-jackson/src/main/java/org/sdase/commons/server/jackson/JacksonConfigurationBundle.java) is
used to configure the JSON serializer. It adds various error mappers to support the SDA error message standard. These
replace the default Dropwizard error mappers but also additional new mappers are added, e.g. mapping JaxRs Exceptions, 
such as NotFound and NotAuthorized. All mappers do log the errors when mapping.
The framework already provides a predefined exception mapper for `InvalidTypeException` if the framework default
for feature `FAIL_ON_INVALID_SUBTYPE` is customized. See section [`Customize the ObjectMapper`](#customize-the-objectmapper) below for details
about customizing the default object mapper settings. If the feature is enabled the [`InvalidTypeIdExceptionMapper`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-server-jackson/src/main/java/org/sdase/commons/server/jackson/errors/InvalidTypeIdExceptionMapper.java)
produces a response in the common error format.

The [`ObjectMapperConfigurationUtil`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-server-jackson/src/main/java/org/sdase/commons/server/jackson/ObjectMapperConfigurationUtil.java)
can be used to receive an `ObjectMapper` with the recommended settings for usage outside a Dropwizard application.  

The default `ObjectMapper` is configured to be fault-tolerant to avoid failures in deserialization. JSR-303 validations
should be used to validate input data. For serialization the bundle disables 
[`FAIL_ON_EMPTY_BEANS`](https://static.javadoc.io/com.fasterxml.jackson.core/jackson-databind/2.9.7/com/fasterxml/jackson/databind/SerializationFeature.html#FAIL_ON_EMPTY_BEANS),
[`WRITE_DATES_AS_TIMESTAMPS`](https://static.javadoc.io/com.fasterxml.jackson.core/jackson-databind/2.9.7/com/fasterxml/jackson/databind/SerializationFeature.html#WRITE_DATES_AS_TIMESTAMPS),
[`WRITE_DURATIONS_AS_TIMESTAMPS`](https://static.javadoc.io/com.fasterxml.jackson.core/jackson-databind/2.9.7/com/fasterxml/jackson/databind/SerializationFeature.html#WRITE_DURATIONS_AS_TIMESTAMPS).
For deserialization the bundle disables 
[`FAIL_ON_UNKNOWN_PROPERTIES`](https://static.javadoc.io/com.fasterxml.jackson.core/jackson-databind/2.9.7/com/fasterxml/jackson/databind/DeserializationFeature.html#FAIL_ON_UNKNOWN_PROPERTIES),
[`FAIL_ON_IGNORED_PROPERTIES`](https://static.javadoc.io/com.fasterxml.jackson.core/jackson-databind/2.9.7/com/fasterxml/jackson/databind/DeserializationFeature.html#FAIL_ON_IGNORED_PROPERTIES),
[`FAIL_ON_INVALID_SUBTYPE`](https://static.javadoc.io/com.fasterxml.jackson.core/jackson-databind/2.9.7/com/fasterxml/jackson/databind/DeserializationFeature.html#FAIL_ON_INVALID_SUBTYPE) 
and enables
[`ACCEPT_SINGLE_VALUE_AS_ARRAY`](https://static.javadoc.io/com.fasterxml.jackson.core/jackson-databind/2.9.7/com/fasterxml/jackson/databind/DeserializationFeature.html#ACCEPT_SINGLE_VALUE_AS_ARRAY),
[`READ_UNKNOWN_ENUM_VALUES_AS_NULL`](https://static.javadoc.io/com.fasterxml.jackson.core/jackson-databind/2.9.7/com/fasterxml/jackson/databind/DeserializationFeature.html#READ_UNKNOWN_ENUM_VALUES_AS_NULL), 
[`READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE`](https://static.javadoc.io/com.fasterxml.jackson.core/jackson-databind/2.9.7/com/fasterxml/jackson/databind/DeserializationFeature.html#READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE).
The `FuzzyEnumModule` from Dropwizard is removed as it lacks support of newer Jackson features for enumerations.


## Usage

In the application class, the bundle is added in the `initialize` method:

```java
import JacksonConfigurationBundle;
import io.dropwizard.core.Application;

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


### Date and Time formatting

It is strongly recommended to use 

- `LocalDate` for dates without time
- `ZonedDateTime` for date and times
- `Duration` for durations with time resolution
- `Period` for durations with day resolution

All these types can be read and written in JSON as ISO 8601 formats. `ZonedDateTime` is formatted with milliseconds or 
nanoseconds according to the detail set in the instance.

| Type            | Resolution    | Example                         |
|-----------------|---------------|---------------------------------|
| `LocalDate`     | Day of Month  | _2018-09-23_                    |
| `ZonedDateTime` | Any time unit | _2018-09-23T14:21:41.123+01:00_ |
| `Duration`      | Any time unit | _P1DT13M_                       |
| `Period`        | Days          | _P1Y2D_                         |

Reading `ZonedDateTime` is configured to be tolerant so that added nanoseconds or missing milliseconds or missing 
seconds are supported.

Please do not use `@JsonFormat(pattern = "...")` for customizing serialization because it breaks tolerant reading of 
formatting variants. If output should be customized, use `@JsonSerializer`.

SDA-Commons provides two default serializers for `ZonedDateTime` to use a fixed resolution in the output.
[Iso8601Serializer](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-server-jackson/src/main/java/org/sdase/commons/server/jackson/Iso8601Serializer.java) is used to omit 
milliseconds and 
[Iso8601Serializer.WithMillis](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-server-jackson/src/main/java/org/sdase/commons/server/jackson/Iso8601Serializer.java#L90) is used to
render the time with 3 digits for milliseconds.

Usage:

```java
class MyResource {
   @JsonSerialize(using = org.sdase.commons.server.jackson.Iso8601Serializer.class)
   private ZonedDateTime zonedDateTime;

   @JsonSerialize(using = org.sdase.commons.server.jackson.Iso8601Serializer.WithMillis.class)
   private ZonedDateTime zonedDateTimeWithMillis;
   
   // ...
}
```


### Adding HAL links to resources

Resources that should be processed for HAL links must be annotated as `@Resource`. Links are added directly in the 
resource class and are annotated as `@Link`. Embedded resources can be added as `@EmbeddedResource`. The 
[Open API Tools](https://github.com/openapi-tools/jackson-dataformat-hal) are used to render them in appropriate 
`_links` and `_embedded` properties. Links are properly documented in Swagger when `io.openapitools.hal:swagger-hal` is
added to the dependencies. `io.openapitools.hal:swagger-hal` is shipped with 
[`sda-commons-server-openapi`](./server-open-api.md).

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
```javascript
GET /persons/123

=> {"_links":{"self":{"href":"/persons/123"}},"name":"John Doe"}
```

#### EmbedHelper

To decide whether a resource is just linked or embedded, the 
[`EmbedHelper`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-server-jackson/src/main/java/org/sdase/commons/server/jackson/EmbedHelper.java) can be used. If query parameters for 
embedding are passed, like `/api/cars?embed=drivers,owner`, `EmbedHelper.isEmbeddingOfRelationRequested(relationName)` 
can be used to check whether a resource should be embedded:

```java
EmbedHelper embedHelper = new EmbedHelper(environment);

...

if (embedHelper.isEmbeddingOfRelationRequested("owner")) {
   carResource.setOwner(createPerson(ownerId));
}
```  

In an application that uses CDI the `EmbedHelper` should be instantiated the same way and provided by a producer method:

```java
   private EmbedHelper embedHelper;

   @Override
   public void run(Configuration config, Environment environment) {
      // ...
      this.embedHelper = new EmbedHelper(environment);
   }

   @Produces
   public EmbedHelper embedHelper() {
      return this.embedHelper;
   }
```


### Field filtering feature for resources

The `JacksonConfigurationBundle` registers the `JacksonFieldFilterModule`, which adds the
`FieldFilterSerializerModifier`. The modifier reads the `fields` query parameter from the JAX-RS request
context. If `fields` is present, only requested fields are rendered. Field names can be comma-separated or
provided through repeated query parameters. Whitespace is ignored.

Field filtering is enabled per serialized type with `@EnableFieldFilter`. If no `fields` parameter is
provided, the complete response is rendered. HAL links and embedded resources are rendered regardless of
the requested fields.

Field filtering support may be disabled in the `JacksonConfigurationBundle.builder()`.

Response fixtures below omit HAL `_links`; field-filtering tests still verify links remain present.

Example model:

```java
import java.util.List;
import java.util.Map;
import org.sdase.commons.server.jackson.EnableFieldFilter;

@EnableFieldFilter(enableNestedPathFiltering = true)
record Person(
    String id,
    String firstName,
    String lastName,
    String nickName,
    List<Child> children,
    Address address,
    Map<String, Attribute> attributes,
    NestedResource renamedCustomProp,
    UnfilteredChild unfilteredChild,
    List<UnfilteredChild> unfilteredChildren) {}

@EnableFieldFilter(enableNestedPathFiltering = true)
record Child(String nickName, String firstName, String lastName) {}

@EnableFieldFilter(enableNestedPathFiltering = true)
record Address(String id, String city) {}

@EnableFieldFilter(enableNestedPathFiltering = true)
record Attribute(String name, String description) {}

@EnableFieldFilter(enableNestedPathFiltering = true)
record NestedResource(String myNestedField, int someNumber, NestedNestedResource myNestedResource) {}

@EnableFieldFilter(enableNestedPathFiltering = true)
record NestedNestedResource(String anotherNestedField, int someNumber) {}

record UnfilteredChild(String name, String lastName) {}
```

To enable field filtering for a resource, annotate the serialized type with `@EnableFieldFilter`.

```http
--8<-- "sda-commons-server-jackson/src/test/resources/field-filtering/basic-fields.http"
```

```json
--8<-- "sda-commons-server-jackson/src/test/resources/field-filtering/basic-fields.json"
```

#### Nested fields

Nested paths use dot notation. Nested filtering must be enabled on every serialized type whose nested
properties should be filtered. This applies to nested objects, list items and map values.

```http
--8<-- "sda-commons-server-jackson/src/test/resources/field-filtering/combined-nested.http"
```

```json
--8<-- "sda-commons-server-jackson/src/test/resources/field-filtering/combined-nested.json"
```

By default, `enableNestedPathFiltering` is `false`. Once a parent property is selected, its complete subtree is
kept. For example:

```http
--8<-- "sda-commons-server-jackson/src/test/resources/field-filtering/parent-subtrees.http"
```

```json
--8<-- "sda-commons-server-jackson/src/test/resources/field-filtering/parent-subtrees.json"
```

If a nested object is not annotated with `@EnableFieldFilter`, selecting one of its sub-fields keeps the
complete nested object. The `enableNestedPathFiltering` setting is evaluated per annotated type.

```http
--8<-- "sda-commons-server-jackson/src/test/resources/field-filtering/unfiltered-child.http"
```

```json
--8<-- "sda-commons-server-jackson/src/test/resources/field-filtering/unfiltered-child.json"
```

The same rule applies to unannotated list items:

```http
--8<-- "sda-commons-server-jackson/src/test/resources/field-filtering/unfiltered-list-child.http"
```

```json
--8<-- "sda-commons-server-jackson/src/test/resources/field-filtering/unfiltered-list-child.json"
```

##### Nested filtering on maps

Requesting `attributes.name` filters each map value. Map keys are not path segments. To filter
fields inside map values, nested filtering must be enabled on the serialized container type and on
the map value type with `@EnableFieldFilter(enableNestedPathFiltering = true)`. If either type does
not enable nested path filtering, the complete map value is kept.

```http
--8<-- "sda-commons-server-jackson/src/test/resources/field-filtering/nested-fields.http"
```

```json
--8<-- "sda-commons-server-jackson/src/test/resources/field-filtering/nested-fields.json"
```

Requesting parent map keeps complete values:

```http
--8<-- "sda-commons-server-jackson/src/test/resources/field-filtering/full-subtree.http"
```

```json
--8<-- "sda-commons-server-jackson/src/test/resources/field-filtering/full-subtree.json"
```

##### Nested filtering on lists

The same rules apply to list items. Requesting a nested path filters each item in the list. To
filter fields inside list items, nested filtering must be enabled on the serialized parent type and
on the list item type with `@EnableFieldFilter(enableNestedPathFiltering = true)`. If either type
does not enable nested path filtering, the complete list item is kept.

```http
--8<-- "sda-commons-server-jackson/src/test/resources/field-filtering/nested-children.http"
```

```json
--8<-- "sda-commons-server-jackson/src/test/resources/field-filtering/nested-children.json"
```

Requesting the parent list keeps the complete subtree of each item.

```http
--8<-- "sda-commons-server-jackson/src/test/resources/field-filtering/parent-children.http"
```

```json
--8<-- "sda-commons-server-jackson/src/test/resources/field-filtering/parent-children.json"
```

HAL links remain included; omitted from fixtures for stable documentation output.

##### Wrapped lists

For wrapped lists, include wrapper property in field path. Wrapper and item types must be annotated
when nested item filtering is enabled.

```http
--8<-- "sda-commons-server-jackson/src/test/resources/field-filtering/wrapped-results.http"
```

```json
--8<-- "sda-commons-server-jackson/src/test/resources/field-filtering/wrapped-results.json"
```

## Configuration

### Disable HAL support

The `JacksonConfigurationBundle` may be initialized without HAL support, if links are not needed or achieved in another
way in the application:

```java
JacksonConfigurationBundle.builder().withoutHalSupport().build();
```

### Customize the `ObjectMapper`

Custom configurations of the `ObjectMapper` can be achieved by adding a customization consumer which receives the used
`ObjectMapper` instance:

```java
JacksonConfigurationBundle.builder()
    .withCustomization(om -> om.enable(SerializationFeature.INDENT_OUTPUT))
    .build();
```

### YAML

If the `JacksonYAMLProvider` is available in the classpath, it will be registered to support requests that 
`Accept application/yaml`. This is especially useful for Swagger which provides the `swagger.json` also as 
`swagger.yaml`.

To activate YAML support, a dependency to `com.fasterxml.jackson.jaxrs:jackson-jaxrs-yaml-provider` has to be added. It
is shipped in an appropriate version with [`sda-commons-server-openapi`](./server-open-api.md).

## Error Format
Exceptions are mapped to a common error format that looks like the following example
```javascript
422 Unprocessable Entity
{
    "title": "Request parameters are not valid",
    "invalidParams": [
         {
            "field": "manufacture",
            "reason": "Audi has no Golf GTI model (not found)"
            "errorCode": "FIELD_CORRELATION_ERROR"
        },
        {
            "field": "model",
            "reason": "Golf GTI is unkown by Audi (not found)"
            "errorCode": "FIELD_CORRELATION_ERROR"
        }
    ]
}
```

For validation errors, the invalidParams section is filled. For other errors, just a title is given.

+ `"field"` defines the invalid field within the JSON structure
+ `"reason"` gives a hint why the value is not valid. This is the error message of the validation.
+ `"errorCode"` is the validation annotation given in uppercase underscore notation 

The reason might be in different language due to internationalization.

Examples how exceptions and the error structure should be used, can be found within the example project 
[`sda-commons-server-errorhandling-example`](./server-error-handling.md)
