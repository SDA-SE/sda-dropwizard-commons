package org.sdase.commons.keymgmt.manager;

import java.util.Set;

/**
 * The <code>KeyManager</code> is used to access key definitions.
 *
 * <p>A key is an enumeration data structure with a list of valid values. Each value has a dedicated
 * business meaning. For example: a key `GENDER` might define the valid values `MALE` and `FEMALE`.
 */
public interface KeyManager {

  /**
   * A set of valid values that are defined for the key
   *
   * @return set with valid values
   */
  Set<String> getValidValues();

  /**
   * states if the testable is within the range of valid values
   *
   * @param testable possible value to be tested
   * @return <code>true</code> if valid, <code>false</code> otherwise
   */
  boolean isValidValue(String testable);
}
