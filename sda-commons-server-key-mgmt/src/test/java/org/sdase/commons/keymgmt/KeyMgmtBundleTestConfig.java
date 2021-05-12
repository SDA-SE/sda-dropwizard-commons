package org.sdase.commons.keymgmt;

import io.dropwizard.Configuration;
import org.sdase.commons.keymgmt.config.KeyMgmtConfig;

public class KeyMgmtBundleTestConfig extends Configuration {

  private KeyMgmtConfig keyMgmt;

  public KeyMgmtConfig getKeyMgmt() {
    return keyMgmt;
  }

  public KeyMgmtBundleTestConfig setKeyMgmt(KeyMgmtConfig keyMgmt) {
    this.keyMgmt = keyMgmt;
    return this;
  }
}
