package org.sdase.commons.shared.asyncapi.jsonschema.victools;

import static com.github.victools.jsonschema.generator.Option.*;
import static com.github.victools.jsonschema.module.jackson.JacksonOption.*;
import static com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS;
import static com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import org.sdase.commons.shared.asyncapi.jsonschema.JsonSchemaBuilder;
import org.sdase.commons.shared.asyncapi.util.RefUtil;

/**
 * A {@link JsonSchemaBuilder} that uses <a
 * href="https://github.com/victools/jsonschema-generator">victools/json-schema-generator</a> to
 * build Json Schemas from Java code.
 */
public class VictoolsJsonSchemaBuilder implements JsonSchemaBuilder {

  private final SchemaGenerator schemaGenerator;

  /**
   * @return a {@link JsonSchemaBuilder} generating schemas suitable for AsyncAPI.
   */
  public static VictoolsJsonSchemaBuilder fromDefaultConfig() {
    var jacksonModule =
        new JacksonModule(
            RESPECT_JSONPROPERTY_ORDER,
            RESPECT_JSONPROPERTY_REQUIRED,
            INLINE_TRANSFORMED_SUBTYPES,
            FLATTENED_ENUMS_FROM_JSONPROPERTY);
    var jakartaValidationModule =
        new JakartaValidationModule(INCLUDE_PATTERN_EXPRESSIONS, NOT_NULLABLE_FIELD_IS_REQUIRED);
    var swagger2Module = new Swagger2Module();
    SchemaGeneratorConfigBuilder configBuilder =
        new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON)
            .with(DEFINITIONS_FOR_ALL_OBJECTS)
            .with(DEFINITION_FOR_MAIN_SCHEMA)
            .with(DEFINITIONS_FOR_MEMBER_SUPERTYPES)
            .with(ALLOF_CLEANUP_AT_THE_END)
            .with(jacksonModule)
            .with(jakartaValidationModule)
            .with(swagger2Module)
            .with(new SwaggerExampleModule())
            .with(new NotBlankModule());
    // see https://github.com/victools/jsonschema-generator/issues/125#issuecomment-657014858
    configBuilder
        .forTypesInGeneral()
        .withPropertySorter((o1, o2) -> 0)
        .withDefinitionNamingStrategy(new DefaultSchemaDefinitionNamingStrategy());

    SchemaGeneratorConfig config = configBuilder.build();
    return new VictoolsJsonSchemaBuilder(config);
  }

  /**
   * A {@link VictoolsJsonSchemaBuilder} with custom configuration.
   *
   * @param schemaGeneratorConfig the configuration
   */
  public VictoolsJsonSchemaBuilder(SchemaGeneratorConfig schemaGeneratorConfig) {
    this.schemaGenerator = new SchemaGenerator(schemaGeneratorConfig);
  }

  @Override
  public Map<String, JsonNode> toJsonSchema(Type type) {
    var schema = toSchemaMap(schemaGenerator.generateSchema(type));
    schema.values().forEach(this::updateRefsInPlace);
    return schema;
  }

  private void updateRefsInPlace(JsonNode jsonSchemaOfType) {
    RefUtil.updateAllRefsRecursively(
        jsonSchemaOfType, this::convertRefToDefinitionsIntoRefToComponentsSchemas);
  }

  private String convertRefToDefinitionsIntoRefToComponentsSchemas(TextNode refValue) {
    String ref = refValue.asText();
    if (ref.startsWith("#/definitions/")) {
      return "#/components/schemas/" + ref.substring("#/definitions/".length());
    } else {
      return ref;
    }
  }

  private Map<String, JsonNode> toSchemaMap(ObjectNode jsonNodesFromVictools) {
    JsonNode generatedDefinitions = jsonNodesFromVictools.get("definitions");
    Map<String, JsonNode> definitions = new LinkedHashMap<>();
    generatedDefinitions
        .fieldNames()
        .forEachRemaining(name -> definitions.put(name, generatedDefinitions.get(name)));
    return definitions;
  }
}
