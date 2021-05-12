package org.sdase.commons.keymgmt.manager;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.sdase.commons.keymgmt.model.KeyDefinition;

public class KeyManagerImpl implements KeyManager {

  private final KeyDefinition key;

  public KeyManagerImpl(KeyDefinition key) {
    this.key = key;
  }

  @Override
  public Set<String> getValidValues() {
    return key.getValuesMap();
  }

  @Override
  public boolean isValidValue(String testable) {
    return getValidValues().contains(testable.toUpperCase(Locale.ROOT));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    KeyManagerImpl that = (KeyManagerImpl) o;
    return key.equals(that.key);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key);
  }
}
