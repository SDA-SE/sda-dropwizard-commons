package org.sdase.commons.server.jackson.test;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NameNotRepeatedValidator
    implements ConstraintValidator<CheckNameRepeated, ValidationResource> {
  @Override
  public void initialize(CheckNameRepeated constraintAnnotation) {
    // Nothing to do here
  }

  @Override
  public boolean isValid(ValidationResource value, ConstraintValidatorContext context) {
    if (value.getFirstName() == null) {
      return true;
    }
    if (value.getFirstName().equalsIgnoreCase(value.getLastName())) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate("First name and last name must be different.")
          // The property firstName is annotated with @JsonProperty("name")
          .addPropertyNode("name")
          .addConstraintViolation();
      context
          .buildConstraintViolationWithTemplate("First name and last name must be different.")
          .addPropertyNode("lastName")
          .addConstraintViolation();
      return false;
    }
    return true;
  }
}
