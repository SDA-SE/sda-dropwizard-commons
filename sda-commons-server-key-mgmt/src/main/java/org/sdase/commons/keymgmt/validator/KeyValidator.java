package org.sdase.commons.keymgmt.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class KeyValidator extends AbstractKeysValidator
    implements ConstraintValidator<PlatformKey, String> {

  @Override
  public void initialize(PlatformKey constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
    super.keyNames = new String[] {constraintAnnotation.value()};
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return super.isValid(value);
  }
}
