package org.sdase.commons.shared.jsonschema.victools;

import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.TypeScope;
import jakarta.validation.constraints.NotBlank;

/** Adds JSON Schema constraints for Jakarta Validation {@link NotBlank}. */
public class NotBlankModule implements Module {

  @Override
  public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
    builder.forTypesInGeneral().withStringPatternResolver(this::stringPatternResolver);
    builder.forTypesInGeneral().withStringMinLengthResolver(this::stringMinLengthResolver);
  }

  private Integer stringMinLengthResolver(TypeScope target) {
    return isAnnotatedWithNotBlank(target) ? 1 : null;
  }

  private String stringPatternResolver(TypeScope target) {
    return isAnnotatedWithNotBlank(target) ? "^.*\\S+.*$" : null;
  }

  private static boolean isAnnotatedWithNotBlank(TypeScope target) {
    return target instanceof FieldScope targetField
        && targetField.getAnnotationConsideringFieldAndGetter(NotBlank.class) != null;
  }
}
