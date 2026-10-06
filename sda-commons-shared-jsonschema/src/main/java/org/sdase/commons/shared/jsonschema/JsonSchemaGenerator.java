package org.sdase.commons.shared.jsonschema;

import static com.github.victools.jsonschema.generator.Option.ALLOF_CLEANUP_AT_THE_END;
import static com.github.victools.jsonschema.generator.Option.DEFINITIONS_FOR_ALL_OBJECTS;
import static com.github.victools.jsonschema.generator.Option.DEFINITIONS_FOR_MEMBER_SUPERTYPES;
import static com.github.victools.jsonschema.generator.Option.DEFINITION_FOR_MAIN_SCHEMA;
import static com.github.victools.jsonschema.module.jackson.JacksonOption.FLATTENED_ENUMS_FROM_JSONPROPERTY;
import static com.github.victools.jsonschema.module.jackson.JacksonOption.INLINE_TRANSFORMED_SUBTYPES;
import static com.github.victools.jsonschema.module.jackson.JacksonOption.RESPECT_JSONPROPERTY_ORDER;
import static com.github.victools.jsonschema.module.jackson.JacksonOption.RESPECT_JSONPROPERTY_REQUIRED;
import static com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS;
import static com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaBuilder;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import java.lang.reflect.Type;
import org.sdase.commons.shared.jsonschema.victools.DefaultSchemaDefinitionNamingStrategy;
import org.sdase.commons.shared.jsonschema.victools.NotBlankModule;
import org.sdase.commons.shared.jsonschema.victools.SwaggerExampleModule;

/** Generates JSON Schema Draft 07 documents for explicit payload, request, and response types. */
public final class JsonSchemaGenerator {

  private static final String DRAFT_7_META_SCHEMA = "http://json-schema.org/draft-07/schema#";

  private final SchemaGenerator schemaGenerator;

  /**
   * Creates generator from Victools configuration.
   *
   * @param schemaGeneratorConfig Victools generator configuration
   */
  public JsonSchemaGenerator(SchemaGeneratorConfig schemaGeneratorConfig) {
    this.schemaGenerator = new SchemaGenerator(requireNonNull(schemaGeneratorConfig));
  }

  /**
   * Creates generator with SDA Commons default configuration.
   *
   * @return JSON Schema Draft 07 generator
   */
  public static JsonSchemaGenerator defaultGenerator() {
    var jacksonModule =
        new JacksonModule(
            RESPECT_JSONPROPERTY_ORDER,
            RESPECT_JSONPROPERTY_REQUIRED,
            INLINE_TRANSFORMED_SUBTYPES,
            FLATTENED_ENUMS_FROM_JSONPROPERTY);
    var jakartaValidationModule =
        new JakartaValidationModule(INCLUDE_PATTERN_EXPRESSIONS, NOT_NULLABLE_FIELD_IS_REQUIRED);
    var configBuilder =
        new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON)
            .with(DEFINITIONS_FOR_ALL_OBJECTS)
            .with(DEFINITION_FOR_MAIN_SCHEMA)
            .with(DEFINITIONS_FOR_MEMBER_SUPERTYPES)
            .with(ALLOF_CLEANUP_AT_THE_END)
            .with(jacksonModule)
            .with(jakartaValidationModule)
            .with(new Swagger2Module())
            .with(new SwaggerExampleModule())
            .with(new NotBlankModule());
    // https://github.com/victools/jsonschema-generator/issues/125#issuecomment-657014858
    configBuilder
        .forTypesInGeneral()
        .withPropertySorter((o1, o2) -> 0)
        .withDefinitionNamingStrategy(new DefaultSchemaDefinitionNamingStrategy());
    return new JsonSchemaGenerator(configBuilder.build());
  }

  /**
   * Generates standalone Draft 07 schema for one explicit payload type.
   *
   * @param root payload Java type
   * @return schema with reusable models under {@code definitions}
   */
  public ObjectNode generate(Type root) {
    return schemaGenerator.generateSchema(requireNonNull(root));
  }

  /**
   * Generates Draft 07 catalog for explicit payload, request, or response types.
   *
   * <p>The catalog root references every supplied type in {@code oneOf}. Consumers requiring one
   * specific type can validate its definition directly, for example {@code
   * schemas.json#/definitions/CustomerCreated}.
   *
   * @param roots payload, request, or response Java types
   * @return catalog with reusable models under {@code definitions}
   */
  public ObjectNode generate(Iterable<? extends Type> roots) {
    requireNonNull(roots);

    SchemaBuilder schemas = schemaGenerator.buildMultipleSchemaDefinitions();
    ArrayNode references = JsonNodeFactory.instance.arrayNode();
    for (Type root : roots) {
      references.add(schemas.createSchemaReference(requireNonNull(root)));
    }

    ObjectNode catalog = JsonNodeFactory.instance.objectNode();
    catalog.put("$schema", DRAFT_7_META_SCHEMA);
    catalog.set("oneOf", references);
    catalog.set("definitions", schemas.collectDefinitions("definitions"));
    return catalog;
  }

  private static <T> T requireNonNull(T value) {
    if (value == null) {
      throw new JsonSchemaGenerationException("JSON Schema generation input must not be null");
    }
    return value;
  }
}
