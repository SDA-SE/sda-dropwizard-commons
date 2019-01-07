package org.sdase.commons.server.errorhandling.rest.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = UpperCaseValidator.class)
@Documented
public @interface UpperCase {

   String message() default "All letters must be UPPER CASE only.";

   Class<?>[] groups() default { };

   Class<? extends Payload>[] payload() default { };

}