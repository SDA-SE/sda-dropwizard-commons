package org.sdase.commons.shared.asyncapi.schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Should we contribute this? This would be a good default behavior for victools.

/**
 * Module that transforms <code>JsonSchemaExamples</code> annotations into examples in the JSON
 * schema.
 */
public class JsonSchemaExamplesModule implements Module {

  private static final Logger LOGGER = LoggerFactory.getLogger(JsonSchemaExamplesModule.class);

  @Override
  public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
    builder.forFields().withInstanceAttributeOverride(this::resolveExampleAttribute);
  }

  private void resolveExampleAttribute(
      ObjectNode collectedMemberAttributes, FieldScope member, SchemaGenerationContext context) {
    // Do not provide examples for items in fake containers, as the examples are already
    // applied to the field itself
    if (!member.isFakeContainerItemScope()) {
      JsonSchemaExamples jsonSchemaExamples =
          member.getAnnotationConsideringFieldAndGetter(JsonSchemaExamples.class);

      if (jsonSchemaExamples != null) {
        ArrayNode examples = context.getGeneratorConfig().createArrayNode();

        for (String json : jsonSchemaExamples.value()) {
          try {
            examples.add(
                context.getGeneratorConfig().getObjectMapper().readValue(json, JsonNode.class));
          } catch (JsonProcessingException e) {
            LOGGER.debug(
                "Unable to parse example value as JSON, using it as string instead: {}",
                e.getMessage());
            // Unable to parse the JSON, in that case we handle it as a raw string, this
            // also makes it easier to pass string examples (without using multiple
            // quotes).
            examples.add(json);
          }
        }

        collectedMemberAttributes.set("examples", examples);
      }
    }
  }
}
