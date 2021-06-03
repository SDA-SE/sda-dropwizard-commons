package org.sdase.commons.keymgmt;

import org.sdase.commons.keymgmt.config.KeyMgmtConfig;
import org.sdase.commons.starter.SdaPlatformConfiguration;

public class KeyMgmtBundleTestConfig extends SdaPlatformConfiguration {

  private KeyMgmtConfig keyMgmt;

  public KeyMgmtConfig getKeyMgmt() {
    return keyMgmt;
  }

  public KeyMgmtBundleTestConfig setKeyMgmt(KeyMgmtConfig keyMgmt) {
    this.keyMgmt = keyMgmt;
    return this;
  }
}
