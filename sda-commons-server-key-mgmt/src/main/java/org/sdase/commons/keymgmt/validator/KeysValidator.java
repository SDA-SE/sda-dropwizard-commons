package org.sdase.commons.keymgmt.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class KeysValidator extends AbstractKeysValidator
    implements ConstraintValidator<PlatformKeys, String> {

  @Override
  public void initialize(PlatformKeys constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
    super.keyNames = constraintAnnotation.values();
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return super.isValid(value);
  }
}
