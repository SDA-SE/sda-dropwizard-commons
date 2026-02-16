/*
 * Copyright 2022- SDA SE Open Industry Solutions (https://www.sda.se)
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.sdase.commons.shared.asyncapi.jsonschema.victools;

import com.fasterxml.classmate.ResolvedType;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.impl.DefinitionKey;
import com.github.victools.jsonschema.generator.naming.SchemaDefinitionNamingStrategy;

// java-classmate changed how generic subtypes are resolved:
// https://github.com/FasterXML/java-classmate/issues/53
// this results that generic type parameters are now part of the naming.
// this class provides a naming without these type parameter.
public class DefaultSchemaDefinitionNamingStrategy implements SchemaDefinitionNamingStrategy {

  @Override
  public String getDefinitionNameForKey(
      DefinitionKey key, SchemaGenerationContext generationContext) {
    ResolvedType type = key.getType();
    Class<?> erasedType = type.getErasedType();
    return erasedType.getSimpleName();
  }
}
