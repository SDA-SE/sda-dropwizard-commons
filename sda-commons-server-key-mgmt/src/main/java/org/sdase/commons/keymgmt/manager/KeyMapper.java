package org.sdase.commons.keymgmt.manager;

/**
 * A <code>KeyMapper</code> allows to map key values between the api specific value and the
 * implementation specific value.
 *
 * <p>Api specific values are the values defined as valid key values within the key management for
 * the apis of the platform. Implementation specific values are the values that are defined within
 * an software component that implements the api.
 */
public interface KeyMapper {

  /**
   * Maps the given value to the implementation specific value
   *
   * @param value api specific key value
   * @return implementation specific key value
   */
  String toImpl(String value);

  /**
   * Maps the given value to the api specific value
   *
   * @param value implementation specific key value
   * @return api specific key value
   */
  String toApi(String value);
}
