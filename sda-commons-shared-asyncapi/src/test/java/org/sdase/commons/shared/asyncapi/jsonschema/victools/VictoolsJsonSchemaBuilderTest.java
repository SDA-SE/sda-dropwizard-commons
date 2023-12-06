package org.sdase.commons.shared.asyncapi.jsonschema.victools;

import java.util.Set;
import org.sdase.commons.shared.asyncapi.jsonschema.AbstractJsonSchemaBuilderTest;

class VictoolsJsonSchemaBuilderTest extends AbstractJsonSchemaBuilderTest {

  public VictoolsJsonSchemaBuilderTest() {
    super(VictoolsJsonSchemaBuilder.fromDefaultConfig());
  }

  @Override
  protected Set<DisabledSpec> disableSpecificFieldTests() {
    return Set.of();
  }
}
