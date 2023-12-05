/*
 * Copyright 2022- SDA SE Open Industry Solutions (https://www.sda.se)
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.sdase.commons.shared.asyncapi.jsonschema.victools;

import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.TypeScope;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;

/** A module that adds the correct {@code format} to {@link URI} related types. */
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
