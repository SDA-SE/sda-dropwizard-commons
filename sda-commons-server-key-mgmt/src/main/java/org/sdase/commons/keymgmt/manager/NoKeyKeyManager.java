package org.sdase.commons.keymgmt.manager;

import java.util.Collections;
import java.util.Set;

/**
 * simulates a non existing key. For a non exiting key, there are no valid values and therefore no
 * testable can be seen as valid.
 */
public class NoKeyKeyManager implements KeyManager {

  @Override
  public Set<String> getValidValues() {
    return Collections.emptySet();
  }

  @Override
  public boolean isValidValue(String testable) {
    return false;
  }
}
