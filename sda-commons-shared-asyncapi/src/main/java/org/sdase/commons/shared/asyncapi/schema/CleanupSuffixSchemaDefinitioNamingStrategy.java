package org.sdase.commons.shared.asyncapi.schema;

import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.impl.DefinitionKey;
import com.github.victools.jsonschema.generator.naming.DefaultSchemaDefinitionNamingStrategy;
import java.util.Map;

public class CleanupSuffixSchemaDefinitioNamingStrategy
    extends DefaultSchemaDefinitionNamingStrategy {

  @Override
  public void adjustDuplicateNames(
      Map<DefinitionKey, String> subschemasWithDuplicateNames,
      SchemaGenerationContext generationContext) {
    int index = subschemasWithDuplicateNames.entrySet().size() - 1;
    for (Map.Entry<DefinitionKey, String> singleEntry : subschemasWithDuplicateNames.entrySet()) {
      if (index == 0) {
        singleEntry.setValue(singleEntry.getValue());
      } else {
        singleEntry.setValue(singleEntry.getValue() + "-" + index);
      }
      --index;
    }
  }
}
