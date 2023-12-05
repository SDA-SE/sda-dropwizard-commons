/*
 * Copyright 2022- SDA SE Open Industry Solutions (https://www.sda.se)
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
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
