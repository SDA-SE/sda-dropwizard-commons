package org.sdase.commons.keymgmt.validator;

import org.sdase.commons.keymgmt.KeyMgmtBundle;

public class KeyMgmtBundleHolder {

  private KeyMgmtBundleHolder() {
    // singleton
  }

  private static KeyMgmtBundle<?> keyMgmtBundle;

  public static KeyMgmtBundle<?> getKeyMgmtBundle() { // NOSONAR
    return keyMgmtBundle;
  }

  public static void setKeyMgmtBundle(KeyMgmtBundle<?> keyMgmtBundle) {
    KeyMgmtBundleHolder.keyMgmtBundle = keyMgmtBundle;
  }
}
