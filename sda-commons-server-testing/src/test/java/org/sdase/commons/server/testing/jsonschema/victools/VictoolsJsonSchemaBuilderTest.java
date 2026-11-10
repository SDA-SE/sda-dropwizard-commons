package org.sdase.commons.server.testing.jsonschema.victools;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.testing.asyncapi.testdata.models.Plane;
import org.sdase.commons.server.testing.jsonschema.AbstractJsonSchemaBuilderTest;

class VictoolsJsonSchemaBuilderTest extends AbstractJsonSchemaBuilderTest {

  public VictoolsJsonSchemaBuilderTest() {
    super(
        org.sdase.commons.server.testing.jsonschema.victools.VictoolsJsonSchemaBuilder
            .fromDefaultConfig());
  }

  @Override
  protected Set<DisabledSpec> disableSpecificFieldTests() {
    return Set.of();
  }

  @Test
  void preservesFieldOrderWithoutAnnotation() {
    var schemas = jsonSchemaBuilder.toJsonSchema(UnorderedFields.class);

    assertThat(schemas.get("UnorderedFields").path("properties").fieldNames())
        .toIterable()
        .containsExactly("zebra", "alpha", "middle");
  }

  @Test
  void honorsExplicitPropertyOrderAndPreservesRemainingFieldOrder() {
    var schemas = jsonSchemaBuilder.toJsonSchema(ExplicitlyOrderedFields.class);

    assertThat(schemas.get("ExplicitlyOrderedFields").path("properties").fieldNames())
        .toIterable()
        .containsExactly("middle", "zebra", "alpha");
  }

  @Test
  void honorsAlphabeticalPropertyOrder() {
    var schemas = jsonSchemaBuilder.toJsonSchema(AlphabeticallyOrderedFields.class);

    assertThat(schemas.get("AlphabeticallyOrderedFields").path("properties").fieldNames())
        .toIterable()
        .containsExactly("alpha", "middle", "zebra");
  }

  @Test
  void shouldNotPrintGenericTypeInName() {
    Map<String, JsonNode> jsonSchema = jsonSchemaBuilder.toJsonSchema(Plane.class);
    assertThat(jsonSchema).containsKey("Plane").doesNotContainKey("Plane(Object)");
  }

  record UnorderedFields(String zebra, String alpha, String middle) {}

  @JsonPropertyOrder({"middle"})
  record ExplicitlyOrderedFields(String zebra, String alpha, String middle) {}

  @JsonPropertyOrder(alphabetic = true)
  record AlphabeticallyOrderedFields(String zebra, String alpha, String middle) {}
}
