package org.sdase.commons.shared.jsonschema.victools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import io.swagger.v3.oas.annotations.media.Schema;

/** Adds {@link Schema#example()} values as JSON Schema {@code examples}. */
public class SwaggerExampleModule implements Module {

  @Override
  public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
    builder.forFields().withInstanceAttributeOverride(this::resolveExampleAttribute);
  }

  private void resolveExampleAttribute(
      ObjectNode collectedMemberAttributes, FieldScope member, SchemaGenerationContext context) {
    if (member.isFakeContainerItemScope()) {
      return;
    }
    Schema schema = member.getAnnotationConsideringFieldAndGetter(Schema.class);
    if (schema == null || schema.example() == null || schema.example().isBlank()) {
      return;
    }
    ArrayNode examples = context.getGeneratorConfig().createArrayNode();
    examples.add(
        isStringType(member, context)
            ? new TextNode(schema.example())
            : readExampleAsJson(context, schema.example()));
    collectedMemberAttributes.set("examples", examples);
  }

  private boolean isStringType(FieldScope member, SchemaGenerationContext context) {
    try {
      return SchemaKeyword.SchemaType.STRING
          .getSchemaKeywordValue()
          .equals(
              context
                  .getGeneratorConfig()
                  .getCustomDefinition(member.getType(), context, null)
                  .getValue()
                  .get("type")
                  .asText());
    } catch (Exception ignored) {
      return false;
    }
  }

  private JsonNode readExampleAsJson(SchemaGenerationContext context, String exampleValue) {
    try {
      return context.getGeneratorConfig().getObjectMapper().readValue(exampleValue, JsonNode.class);
    } catch (JsonProcessingException ignored) {
      return new TextNode(exampleValue);
    }
  }
}
