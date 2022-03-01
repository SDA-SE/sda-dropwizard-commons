package org.sdase.commons.keymgmt.validator;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import org.sdase.commons.keymgmt.model.KeyDefinition;

/**
 * The annotated element is a <i>PlatformKeys</i> and must therefore contain a valid value
 * corresponding to the key definitions. The values of the annotation represents the names of keys.
 * {@link KeyDefinition#getName()}.
 *
 * <p>The validator checks if this value is defined by using the {@link
 * org.sdase.commons.keymgmt.KeyMgmtBundle}. and creates a {@link
 * org.sdase.commons.keymgmt.manager.KeyMapper} for the defined key definition.
 *
 * <p>The validator accepts <code>null</code> values.
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = KeysValidator.class)
@Documented
public @interface PlatformKeys {

  /**
   * The values represent an array with key definition names of the key used within the validation.
   * {@link KeyDefinition#getName()}
   */
  String[] values();

  String message() default "The attribute does not contain a valid platform key value.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
