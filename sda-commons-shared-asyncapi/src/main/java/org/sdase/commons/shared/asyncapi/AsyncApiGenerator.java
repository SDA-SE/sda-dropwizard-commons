package org.sdase.commons.shared.asyncapi;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.sdase.commons.shared.asyncapi.internal.JsonSchemaEmbedder;
import org.sdase.commons.shared.yaml.YamlUtil;

/**
 * Generator used to build AsyncAPI specs from a template base file and schemas generated from code.
 * The schemas are referenced via absolute $refs like
 *
 * <pre>
 *     payload:
 *       $ref: './partner-streaming-event.json#/definitions/CreateNaturalPersonEvent'
 * </pre>
 *
 * These referenced files are automatically created from Java classes annotated with Jackson and
 * mbknor-jackson-jsonSchema annotations. Afterwards everything is embedded into a single
 * self-contained file
 */
public class AsyncApiGenerator {

  private AsyncApiGenerator() {
    // No public constructor
  }

  /**
   * Creates a new generator for AsyncAPI specs
   *
   * @return builder
   */
  public static AsyncApiBaseBuilder builder() {
    return new Builder();
  }

  public interface AsyncApiBaseBuilder {

    /**
     * Supply a base AsyncAPI file to be used as a template
     *
     * @param url The resource url to the template file
     * @return builder
     */
    SchemaBuilder withAsyncApiBase(URL url);
  }

  public interface SchemaBuilder extends FinalBuilder {

    /**
     * Supply a JSON schema generate from clazz to the AsyncAPI.
     *
     * @param name The name of the JSON file under which the schema is referenced in the template
     *     file
     * @param clazz The class to generate a schema for
     * @param <T> The type of clazz
     * @return builder
     */
    <T> SchemaBuilder withSchema(String name, Class<T> clazz);

    /**
     * Supply a JSON schema from an existing JSON node
     *
     * @param name The name of the JSON file under which the schema is referenced in the template *
     *     file
     * @param node The node to embed
     * @return builder
     */
    SchemaBuilder withSchema(String name, JsonNode node);
  }

  public interface FinalBuilder {

    /**
     * Generates a new AsyncAPI spec based on the supplied builder parameters.
     *
     * @return A JSON object for the AsyncAPI spec.
     */
    JsonNode generate();

    /**
     * Generates a new AsyncAPI spec based on the supplied builder parameters.
     *
     * @return A YAML encoded AsyncAPI spec.
     */
    String generateYaml();
  }

  private static class Builder implements AsyncApiBaseBuilder, SchemaBuilder {
    private JsonNode asyncApiBaseTemplate;
    private final Map<String, JsonNode> schemas = new HashMap<>();
    private final JsonSchemaEmbedder jsonSchemaEmbedder =
        new JsonSchemaEmbedder("/components/schemas", schemas::get);

    @Override
    public SchemaBuilder withAsyncApiBase(URL url) {
      asyncApiBaseTemplate = YamlUtil.load(url, JsonNode.class);
      return this;
    }

    @Override
    public <T> SchemaBuilder withSchema(String name, Class<T> clazz) {
      schemas.put(
          name,
          JsonSchemaGenerator.builder().forClass(clazz).allowAdditionalProperties(true).generate());

      return this;
    }

    @Override
    public SchemaBuilder withSchema(String name, JsonNode node) {
      schemas.put(name, node);

      return this;
    }

    @Override
    public JsonNode generate() {
      return jsonSchemaEmbedder.resolve(asyncApiBaseTemplate);
    }

    @Override
    public String generateYaml() {
      return YamlUtil.writeValueAsString(generate());
    }
  }
}
