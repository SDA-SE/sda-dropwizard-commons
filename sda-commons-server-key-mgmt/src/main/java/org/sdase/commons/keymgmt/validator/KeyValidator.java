package org.sdase.commons.keymgmt.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.sdase.commons.keymgmt.KeyMgmtBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyValidator implements ConstraintValidator<PlatformKey, String> {

  private static final Logger LOG = LoggerFactory.getLogger(KeyValidator.class);
  private String keyName;

  @Override
  public void initialize(PlatformKey constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
    keyName = constraintAnnotation.value();
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    // The validator should not enforce a mandatory field
    if (value == null) {
      return true;
    }

    KeyMgmtBundle<?> keyMgmtBundle = KeyMgmtBundleHolder.getKeyMgmtBundle();
    if (keyMgmtBundle == null) {
      LOG.warn("Validation of messages attributes before key-mgmt-bundle is available");
      return false;
    } else {
      if (keyMgmtBundle.isValueValidationEnabled()) {
        return keyMgmtBundle.createKeyManager(keyName).isValidValue(value);
      }
      return true;
    }
  }
}
