package org.sdase.commons.shared.asyncapi.jsonschema.victools;

import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.TypeScope;
import jakarta.validation.constraints.NotBlank;

/**
 * Adds JSON Schema constraints for Jakarta Validation {@link NotBlank}.
 *
 * @deprecated This class will be moved to server-testing with the next major update. It is assumed
 *     that no direct access from consumers is required. In case this assumption is wrong, a
 *     migration path will be provided with the next major update. Reach out to the maintainers in
 *     case migrating with the next major is not an option.
 */
@Deprecated(forRemoval = true)
public class NotBlankModule implements Module {

  @Override
  public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
    builder.forTypesInGeneral().withStringPatternResolver(this::stringPatternResolver);
    builder.forTypesInGeneral().withStringMinLengthResolver(this::stringMinLengthResolver);
  }

  private Integer stringMinLengthResolver(TypeScope target) {
    if (isAnnotatedWithNotBlank(target)) {
      return 1;
    }
    return null;
  }

  private String stringPatternResolver(TypeScope target) {
    if (isAnnotatedWithNotBlank(target)) {
      return "^.*\\S+.*$";
    }
    return null;
  }

  private static boolean isAnnotatedWithNotBlank(TypeScope target) {
    return target instanceof FieldScope targetField
        && (targetField.getAnnotationConsideringFieldAndGetter(NotBlank.class) != null);
  }
}
