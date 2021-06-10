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
 * The annotated element is a <i>PlatformKey</i> and must therefore contain a valid value
 * corresponding to the key definition. The value of the annotation represents the name of the key
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
@Constraint(validatedBy = KeyValidator.class)
@Documented
public @interface PlatformKey {

  /**
   * The value represents the key definition name of the key used within the validation. {@link
   * KeyDefinition#getName()}
   */
  String value();

  String message() default "The attribute does not contain a valid platform key value.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
