package org.sdase.commons.keymgmt.model;

import javax.validation.constraints.NotEmpty;

@SuppressWarnings("unused")
public class KeyDefinitionValue {

  @NotEmpty private String value;

  private String description;

  public String getValue() {
    return value;
  }

  public KeyDefinitionValue setValue(String value) {
    this.value = value;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public KeyDefinitionValue setDescription(String description) {
    this.description = description;
    return this;
  }
}
