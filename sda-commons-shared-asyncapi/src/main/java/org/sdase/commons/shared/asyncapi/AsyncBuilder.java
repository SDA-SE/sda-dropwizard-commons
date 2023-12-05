/*
 * Copyright 2022- SDA SE Open Industry Solutions (https://www.sda.se)
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.sdase.commons.shared.asyncapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sdase.commons.shared.asyncapi.jsonschema.JsonSchemaBuilder;
import org.sdase.commons.shared.asyncapi.jsonschema.victools.VictoolsJsonSchemaBuilder;
import org.sdase.commons.shared.asyncapi.util.JsonNodeUtil;
import org.sdase.commons.shared.asyncapi.util.RefUtil;

public class AsyncBuilder
    implements AsyncApiGenerator.AsyncApiBaseBuilder,
        AsyncApiGenerator.SchemaBuilder,
        FinalBuilder {

  private JsonNode asyncApiBaseTemplate;
  private JsonSchemaBuilder jsonSchemaBuilder;

  public AsyncBuilder() {
    this.jsonSchemaBuilder = VictoolsJsonSchemaBuilder.fromDefaultConfig();
  }

  @Override
  public AsyncApiGenerator.SchemaBuilder withAsyncApiBase(String yamlAsyncApiContent) {
    try {
      asyncApiBaseTemplate = YAMLMapper.builder().build().readTree(yamlAsyncApiContent);
    } catch (IOException e) {
      throw new UncheckedIOException("Error while converting YAML to JSONNode", e);
    }
    return this;
  }

  @Override
  public FinalBuilder withJsonSchemaBuilder(JsonSchemaBuilder jsonSchemaBuilder) {
    this.jsonSchemaBuilder = jsonSchemaBuilder;
    return this;
  }

  @Override
  public JsonNode generate() {
    ObjectNode asyncApiObject = asyncApiBaseTemplate.deepCopy();
    var jsonSchemas = createSchemasFromReferencedClasses(asyncApiObject);
    insertSchemas(jsonSchemas, asyncApiObject);
    JsonNodeUtil.sortJsonNodeInPlace(asyncApiObject.at("/components/schemas"));
    return asyncApiObject;
  }

  private Map<String, JsonNode> createSchemasFromReferencedClasses(ObjectNode asyncApi) {
    var types =
        findRequiredSchemas(asyncApi).stream().map(this::toType).collect(Collectors.toSet());
    return jsonSchemaBuilder.toJsonSchema(types);
  }

  private Set<String> findRequiredSchemas(JsonNode asyncApiJsonNode) {
    String prefix = "class://";
    Set<String> result = new LinkedHashSet<>();
    RefUtil.updateAllRefsRecursively(
        asyncApiJsonNode,
        ref -> {
          String refText = ref.asText();
          if (refText.startsWith(prefix)) {
            result.add(refText.substring(prefix.length()));
            String simpleClassName = refText.substring(refText.lastIndexOf(".") + 1);
            return "#/components/schemas/%s".formatted(simpleClassName);
          }
          return refText;
        });
    return result;
  }

  private void insertSchemas(Map<String, JsonNode> newSchemas, ObjectNode targetAsyncApiObject) {
    ObjectNode components = getOrCreateObject(targetAsyncApiObject, "components");
    ObjectNode targetSchemas = getOrCreateObject(components, "schemas");
    newSchemas.forEach(targetSchemas::set);
  }

  private ObjectNode getOrCreateObject(ObjectNode source, String fieldName) {
    if (source.has(fieldName)) {
      JsonNode jsonNode = source.get(fieldName);
      if (jsonNode instanceof ObjectNode objectNode) {
        return objectNode;
      }
      throw new IllegalStateException(
          "'%s' is not an Object node but a %s in %s"
              .formatted(fieldName, jsonNode.getClass().getSimpleName(), source));
    }
    ObjectNode newObjectNode = source.objectNode();
    source.set(fieldName, newObjectNode);
    return newObjectNode;
  }

  private Type toType(String fullyQualifiedClassName) {
    try {
      return getClass().getClassLoader().loadClass(fullyQualifiedClassName);
    } catch (ClassNotFoundException e) {
      throw new ReferencedClassNotFoundException(e);
    }
  }
}
