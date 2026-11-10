package org.sdase.commons.server.testing.jsonschema;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jackson.JsonPropertySorter;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.sdase.commons.server.testing.jsonschema.victools.DefaultSchemaDefinitionNamingStrategy;
import org.sdase.commons.server.testing.jsonschema.victools.NotBlankModule;
import org.sdase.commons.server.testing.jsonschema.victools.SwaggerExampleModule;

/**
 * Generates JSON Schema Draft 2020-12 documents for explicit payload, request, and response types.
 */
public final class JsonSchemaGenerator {

  private static final SchemaVersion SCHEMA_VERSION = SchemaVersion.DRAFT_2020_12;
  private final SchemaGenerator schemaGenerator;

  /**
   * Creates generator from Victools configuration.
   *
   * @param schemaGeneratorConfig Victools generator configuration
   */
  private JsonSchemaGenerator(SchemaGeneratorConfig schemaGeneratorConfig) {
    this.schemaGenerator = new SchemaGenerator(requireNonNull(schemaGeneratorConfig));
  }

  /**
   * Creates generator with SDA Commons default configuration.
   *
   * @return JSON Schema Draft 2020-12 generator
   */
  public static JsonSchemaGenerator defaultGenerator() {
    var jacksonModule =
        new JacksonModule(
            JacksonOption.RESPECT_JSONPROPERTY_ORDER,
            JacksonOption.RESPECT_JSONPROPERTY_REQUIRED,
            JacksonOption.INLINE_TRANSFORMED_SUBTYPES,
            JacksonOption.FLATTENED_ENUMS_FROM_JSONPROPERTY);
    var jakartaValidationModule =
        new JakartaValidationModule(
            JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS,
            JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED);
    var configBuilder =
        new SchemaGeneratorConfigBuilder(SCHEMA_VERSION, OptionPreset.PLAIN_JSON)
            .with(Option.DEFINITIONS_FOR_ALL_OBJECTS)
            .with(Option.DEFINITION_FOR_MAIN_SCHEMA)
            .with(Option.DEFINITIONS_FOR_MEMBER_SUPERTYPES)
            .with(Option.ALLOF_CLEANUP_AT_THE_END)
            .with(jacksonModule)
            .with(jakartaValidationModule)
            .with(new Swagger2Module())
            .with(new SwaggerExampleModule())
            .with(new NotBlankModule());
    // Honor @JsonPropertyOrder while preserving discovered field order when it is absent.
    configBuilder
        .forTypesInGeneral()
        .withPropertySorter(new JsonPropertySorter(false))
        .withDefinitionNamingStrategy(new DefaultSchemaDefinitionNamingStrategy());
    return new JsonSchemaGenerator(configBuilder.build());
  }

  /**
   * Generates standalone Draft 2020-12 schema for one explicit payload type.
   *
   * @param root payload Java type
   * @return schema with reusable models under {@code $defs}
   */
  public ObjectNode generate(Type root) {
    return generate(root, null);
  }

  /**
   * Generates standalone Draft 2020-12 schema for one explicit payload type with an optional
   * root-level identifier.
   *
   * @param root payload Java type
   * @param id identifier written to the generated schema root, or {@code null} to omit it
   * @return schema with reusable models under {@code $defs}
   */
  public ObjectNode generate(Type root, String id) {
    ObjectNode schema = schemaGenerator.generateSchema(requireNonNull(root));
    if (id != null) {
      schema.put("$id", id);
    }
    return schema;
  }

  /**
   * Generates Draft 2020-12 catalog for explicit payload, request, or response types.
   *
   * <p>The catalog root references every supplied type in {@code oneOf}. Consumers requiring one
   * specific type can validate its definition directly, for example {@code
   * schemas.json#/$defs/CustomerCreated}. Duplicate roots are ignored. One distinct root produces a
   * standalone schema without {@code oneOf}.
   *
   * @param roots payload, request, or response Java types
   * @return catalog with reusable models under {@code $defs}
   */
  public ObjectNode generate(Iterable<? extends Type> roots) {
    return generate(roots, null);
  }

  /**
   * Generates Draft 2020-12 catalog for explicit payload, request, or response types with an
   * optional root-level identifier.
   *
   * <p>The catalog root references every supplied type in {@code oneOf}. Consumers requiring one
   * specific type can validate its definition directly, for example {@code
   * schemas.json#/$defs/CustomerCreated}. Duplicate roots are ignored. One distinct root produces a
   * standalone schema without {@code oneOf}.
   *
   * @param roots payload, request, or response Java types
   * @param id identifier written to the generated schema root, or {@code null} to omit it
   * @return catalog with reusable models under {@code $defs}
   */
  public ObjectNode generate(Iterable<? extends Type> roots, String id) {
    requireNonNull(roots);

    Set<Type> distinctRoots =
        StreamSupport.stream(roots.spliterator(), false)
            .map(JsonSchemaGenerator::requireNonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (distinctRoots.size() == 1) {
      return generate(distinctRoots.iterator().next(), id);
    }

    SchemaBuilder schemas = schemaGenerator.buildMultipleSchemaDefinitions();
    ArrayNode references = JsonNodeFactory.instance.arrayNode();
    distinctRoots.stream().map(schemas::createSchemaReference).forEach(references::add);

    ObjectNode catalog = JsonNodeFactory.instance.objectNode();
    catalog.put("$schema", SCHEMA_VERSION.getIdentifier());
    if (id != null) {
      catalog.put("$id", id);
    }
    catalog.set("oneOf", references);
    catalog.set("$defs", schemas.collectDefinitions("$defs"));
    return catalog;
  }

  private static <T> T requireNonNull(T value) {
    if (value == null) {
      throw new JsonSchemaGenerationException("JSON Schema generation input must not be null");
    }
    return value;
  }
}
