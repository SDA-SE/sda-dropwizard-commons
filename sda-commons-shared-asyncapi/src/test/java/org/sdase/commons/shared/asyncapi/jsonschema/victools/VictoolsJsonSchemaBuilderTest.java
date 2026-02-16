package org.sdase.commons.shared.asyncapi.jsonschema.victools;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sdase.commons.shared.asyncapi.jsonschema.AbstractJsonSchemaBuilderTest;
import org.sdase.commons.shared.asyncapi.test.data.models.Plane;

class VictoolsJsonSchemaBuilderTest extends AbstractJsonSchemaBuilderTest {

  public VictoolsJsonSchemaBuilderTest() {
    super(VictoolsJsonSchemaBuilder.fromDefaultConfig());
  }

  @Override
  protected Set<DisabledSpec> disableSpecificFieldTests() {
    return Set.of();
  }

  @Test
  void shouldNotPrintGenericTypeInName() {
    Map<String, JsonNode> jsonSchema = jsonSchemaBuilder.toJsonSchema(Plane.class);
    assertThat(jsonSchema).containsKey("Plane").doesNotContainKey("Plane(Object)");
  }
}
