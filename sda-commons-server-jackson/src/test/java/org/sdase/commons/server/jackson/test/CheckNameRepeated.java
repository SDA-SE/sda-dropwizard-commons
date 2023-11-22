package org.sdase.commons.server.jackson.test;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = NameNotRepeatedValidator.class)
@Documented
public @interface CheckNameRepeated {

  String message() default "First and lastname must  not repeat itself";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
