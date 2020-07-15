package org.sdase.commons.shared.asyncapi;

import static com.github.victools.jsonschema.generator.Option.ALLOF_CLEANUP_AT_THE_END;
import static com.github.victools.jsonschema.generator.Option.DEFINITIONS_FOR_ALL_OBJECTS;
import static com.github.victools.jsonschema.generator.Option.DEFINITION_FOR_MAIN_SCHEMA;
import static com.github.victools.jsonschema.generator.Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT;
import static com.github.victools.jsonschema.generator.OptionPreset.PLAIN_JSON;
import static com.github.victools.jsonschema.generator.SchemaVersion.DRAFT_7;
import static com.github.victools.jsonschema.module.jackson.JacksonOption.FLATTENED_ENUMS_FROM_JSONPROPERTY;
import static com.github.victools.jsonschema.module.jackson.JacksonOption.IGNORE_TYPE_INFO_TRANSFORM;
import static com.github.victools.jsonschema.module.jackson.JacksonOption.RESPECT_JSONPROPERTY_ORDER;
import static com.github.victools.jsonschema.module.jackson.JacksonOption.SKIP_SUBTYPE_LOOKUP;
import static com.github.victools.jsonschema.module.javax.validation.JavaxValidationOption.INCLUDE_PATTERN_EXPRESSIONS;
import static com.github.victools.jsonschema.module.javax.validation.JavaxValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.naming.CleanSchemaDefinitionNamingStrategy;
import com.github.victools.jsonschema.generator.naming.DefaultSchemaDefinitionNamingStrategy;
import com.github.victools.jsonschema.generator.naming.SchemaDefinitionNamingStrategy;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.javax.validation.JavaxValidationModule;
import org.sdase.commons.server.jackson.ObjectMapperConfigurationUtil;
import org.sdase.commons.shared.asyncapi.schema.CleanupSuffixSchemaDefinitioNamingStrategy;
import org.sdase.commons.shared.asyncapi.schema.JsonSchemaExamplesModule;
import org.sdase.commons.shared.asyncapi.schema.TemporalFormatModule;
import org.sdase.commons.shared.yaml.YamlUtil;

/** Generator for JSON schemas from Jackson and mbknor-jackson-jsonSchema annotated Java classes. */
public class JsonSchemaGenerator {

  private JsonSchemaGenerator() {
    // No public constructor
  }

  /**
   * Creates a new generator for JSON schemas
   *
   * @return builder
   */
  public static SchemaBuilder builder() {
    return new Builder();
  }

  public interface SchemaBuilder {

    /**
     * Includes a class into the schema.
     *
     * @param clazz The class to include
     * @param <T> The type of the class.
     * @return builder
     */
    <T> AdditionalPropertiesBuilder forClass(Class<T> clazz);
  }

  public interface FinalBuilder {

    /**
     * Generates a new JSON schema for the supplied class.
     *
     * @return A JSON object for the JSON schema.
     */
    JsonNode generate();

    /**
     * Generates a new JSON schema for the supplied class.
     *
     * @return A YAML representation for the JSON schema.
     */
    String generateYaml();
  }

  public interface AdditionalPropertiesBuilder extends FinalBuilder {

    /**
     * Whether the additionalProperties option in the generated schema is enabled.
     *
     * @param enabled If true, additionProperties is true.
     * @return builder
     */
    FinalBuilder allowAdditionalProperties(boolean enabled);
  }

  private static class Builder implements SchemaBuilder, AdditionalPropertiesBuilder, FinalBuilder {

    boolean allowAdditionalPropertiesEnabled = false;
    Class<?> clazz;

    @Override
    public <T> AdditionalPropertiesBuilder forClass(Class<T> clazz) {
      this.clazz = clazz;
      return this;
    }

    @Override
    public JsonNode generate() {
      ObjectMapper objectMapper = ObjectMapperConfigurationUtil.configureMapper().build();
      JacksonModule jacksonModule =
          new JacksonModule(
              RESPECT_JSONPROPERTY_ORDER,
              //IGNORE_TYPE_INFO_TRANSFORM,
              //SKIP_SUBTYPE_LOOKUP,
              FLATTENED_ENUMS_FROM_JSONPROPERTY);
      JavaxValidationModule javaxValidationModule =
          new JavaxValidationModule(INCLUDE_PATTERN_EXPRESSIONS, NOT_NULLABLE_FIELD_IS_REQUIRED);
      JsonSchemaExamplesModule jsonSchemaExamplesModule = new JsonSchemaExamplesModule();
      TemporalFormatModule temporalFormatModule = new TemporalFormatModule();
      // We use JSON schema draft 07 here explicitly and not the latest version, as
      // AsyncAPI uses DRAFT 07:
      // https://www.asyncapi.com/docs/specifications/2.0.0/#a-name-messageobjectschemaformattable-a-schema-formats-table
      SchemaGeneratorConfigBuilder configBuilder =
          new SchemaGeneratorConfigBuilder(objectMapper, DRAFT_7, PLAIN_JSON)
              .with(DEFINITIONS_FOR_ALL_OBJECTS)
              .with(DEFINITION_FOR_MAIN_SCHEMA)
              .with(ALLOF_CLEANUP_AT_THE_END)
              .with(jacksonModule)
              .with(javaxValidationModule)
              .with(jsonSchemaExamplesModule)
              .with(temporalFormatModule);

      // TODO: While this workaround generates a schema that we can use with the AsyncAPIGenerator,
      //  the generated schema with the allOf definition doesn't look useful in the asyncapi docs...
      configBuilder
          .forTypesInGeneral()
          .withDefinitionNamingStrategy(new CleanupSuffixSchemaDefinitioNamingStrategy());

      // TODO: This is behavior I would expect from the JacksonModule, should we contribute it?
      configBuilder
          .forFields()
          .withRequiredCheck(
              member -> {
                JsonProperty jsonProperty =
                    member.getAnnotationConsideringFieldAndGetter(JsonProperty.class);

                return jsonProperty != null && jsonProperty.required();
              });

      // TODO: Support defaultImpl? (NEW FEATURE, SEPARATE COMMIT once we finished the rest)
      //  This is the main reason for evaluating if victools is easier to extend, it's possible with
      //  the existing one, but not "nice". defaultImpl is not supported by victools out of the box.
      //  defaultImpl means, that there is another type that the JSON can be parsed into, if it
      // lacks
      //  a known type annotation. This can be handled using an anyOf and including only fields of
      //  defaultImpl (without sub types).

      if (!allowAdditionalPropertiesEnabled) {
        configBuilder.with(FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT);
      }

      SchemaGeneratorConfig config = configBuilder.build();
      SchemaGenerator generator = new SchemaGenerator(config);
      return generator.generateSchema(clazz);
    }

    @Override
    public String generateYaml() {
      return YamlUtil.writeValueAsString(generate());
    }

    @Override
    public FinalBuilder allowAdditionalProperties(boolean enabled) {
      this.allowAdditionalPropertiesEnabled = enabled;
      return this;
    }
  }
}
