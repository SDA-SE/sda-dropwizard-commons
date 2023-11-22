package org.sdase.commons.server.errorhandling.rest.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Locale;

public class UpperCaseValidator implements ConstraintValidator<UpperCase, String> {

  @Override
  public void initialize(UpperCase constraintAnnotation) {
    // nothing to do here
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    return value.equals(value.toUpperCase(Locale.ROOT)); // NOSONAR
  }
}
