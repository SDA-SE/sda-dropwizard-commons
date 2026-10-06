package org.sdase.commons.shared.jsonschema.victools;

import com.fasterxml.classmate.ResolvedType;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.impl.DefinitionKey;
import com.github.victools.jsonschema.generator.naming.SchemaDefinitionNamingStrategy;

/**
 * Uses erased simple class names to keep generated definition names stable across ClassMate
 * versions.
 */
public class DefaultSchemaDefinitionNamingStrategy implements SchemaDefinitionNamingStrategy {

  @Override
  public String getDefinitionNameForKey(
      DefinitionKey key, SchemaGenerationContext generationContext) {
    ResolvedType type = key.getType();
    return type.getErasedType().getSimpleName();
  }
}
