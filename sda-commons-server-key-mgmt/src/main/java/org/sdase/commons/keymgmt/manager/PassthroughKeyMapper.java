package org.sdase.commons.keymgmt.manager;

public class PassthroughKeyMapper implements KeyMapper {

  @Override
  public String toImpl(String value) {
    return value;
  }

  @Override
  public String toApi(String value) {
    return value;
  }
}
