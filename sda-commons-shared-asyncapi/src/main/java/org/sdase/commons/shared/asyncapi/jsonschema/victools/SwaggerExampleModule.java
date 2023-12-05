/*
 * Copyright 2022- SDA SE Open Industry Solutions (https://www.sda.se)
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.sdase.commons.shared.asyncapi.jsonschema.victools;

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

/**
 * A module that picks up {@link Schema#example()} from properties and adds it as item in the {@code
 * examples} array of the definition of that property. Examples are parsed as Json unless the
 * property type is {@code string}. The raw string value is used as fallback when it's not parseable
 * as Json.
 */
// In the long term, we should contribute a better solution for examples as suggested here:
// https://github.com/SDA-SE/sda-dropwizard-commons/pull/409/files#r517230013
// (This code is inspired by that PR)
public class SwaggerExampleModule implements Module {

  @Override
  public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
    builder.forFields().withInstanceAttributeOverride(this::resolveExampleAttribute);
  }

  private void resolveExampleAttribute(
      ObjectNode collectedMemberAttributes, FieldScope member, SchemaGenerationContext context) {
    // Do not provide examples for items in fake containers, as the examples are already
    // applied to the field itself
    if (member.isFakeContainerItemScope()) {
      return;
    }
    Schema schema = member.getAnnotationConsideringFieldAndGetter(Schema.class);
    if (schema == null) {
      return;
    }
    String exampleValue = schema.example();
    if (exampleValue == null || exampleValue.isBlank()) {
      return;
    }
    ArrayNode examples = context.getGeneratorConfig().createArrayNode();
    if (isStringType(member, context)) {
      examples.add(exampleValue);
    } else {
      examples.add(readExampleAsJson(context, exampleValue));
    }
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
      // Unable to parse the JSON, in that case we handle it as a raw string, this
      // also makes it easier to pass string examples (without using multiple
      // quotes).
      return new TextNode(exampleValue);
    }
  }
}
