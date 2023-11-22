package org.sdase.commons.keymgmt.model;

import jakarta.validation.constraints.NotEmpty;
import java.util.Locale;

@SuppressWarnings("unused")
public class KeyMappingModel {

  @NotEmpty private String name;

  private ValueMappingModel mapping;

  public String getName() {
    return name;
  }

  public KeyMappingModel setName(String name) {
    this.name = name.toUpperCase(Locale.ROOT);
    return this;
  }

  public ValueMappingModel getMapping() {
    return mapping;
  }

  public KeyMappingModel setMapping(ValueMappingModel mapping) {
    this.mapping = mapping;
    return this;
  }
}
