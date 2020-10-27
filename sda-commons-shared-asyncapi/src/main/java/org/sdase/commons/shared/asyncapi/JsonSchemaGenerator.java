package org.sdase.commons.shared.asyncapi;

import static org.sdase.commons.shared.asyncapi.internal.JsonNodeUtil.sortJsonNodeInPlace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjetland.jackson.jsonSchema.JsonSchemaConfig;
import com.kjetland.jackson.jsonSchema.JsonSchemaDraft;
import org.sdase.commons.server.jackson.ObjectMapperConfigurationUtil;
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
      com.kjetland.jackson.jsonSchema.JsonSchemaGenerator jsonSchemaGenerator =
          new com.kjetland.jackson.jsonSchema.JsonSchemaGenerator(
              objectMapper,
              JsonSchemaConfig.vanillaJsonSchemaDraft4()
                  // We use JSON schema draft 07 here explicitly and not the latest version, as
                  // AsyncAPI uses DRAFT 07:
                  // https://www.asyncapi.com/docs/specifications/2.0.0/#a-name-messageobjectschemaformattable-a-schema-formats-table
                  .withJsonSchemaDraft(JsonSchemaDraft.DRAFT_07)
                  .withFailOnUnknownProperties(!allowAdditionalPropertiesEnabled));
      JsonNode jsonNode = jsonSchemaGenerator.generateJsonSchema(clazz);
      sortJsonNodeInPlace(jsonNode.at("/definitions"));
      return jsonNode;
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
