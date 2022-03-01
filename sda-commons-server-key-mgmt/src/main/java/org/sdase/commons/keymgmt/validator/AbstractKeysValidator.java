package org.sdase.commons.keymgmt.validator;

import org.sdase.commons.keymgmt.KeyMgmtBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractKeysValidator {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractKeysValidator.class);
  protected String[] keyNames;

  public boolean isValid(String value) {
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
        boolean isValid = false;
        for (String keyName : keyNames) {
          isValid = isValid || keyMgmtBundle.createKeyManager(keyName).isValidValue(value);
        }
        return isValid;
      }
      return true;
    }
  }
}
