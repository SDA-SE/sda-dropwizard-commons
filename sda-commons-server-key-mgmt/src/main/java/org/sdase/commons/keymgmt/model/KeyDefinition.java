package org.sdase.commons.keymgmt.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.*;
import javax.validation.constraints.NotEmpty;

@SuppressWarnings("unused")
public class KeyDefinition {

  @NotEmpty private String name;

  private String description;

  private final Map<String, KeyDefinitionValue> valuesMap = new HashMap<>();

  public String getName() {
    return name;
  }

  public KeyDefinition setName(String name) {
    this.name = name.toUpperCase(Locale.ROOT);
    return this;
  }

  public String getDescription() {
    return description;
  }

  public KeyDefinition setDescription(String description) {
    this.description = description;
    return this;
  }

  @JsonSetter("values")
  public KeyDefinition setValuesList(List<KeyDefinitionValue> values) {
    values.forEach(v -> this.valuesMap.put(v.getValue().toUpperCase(Locale.ROOT), v));
    return this;
  }

  public Set<String> getValuesMap() {
    return this.valuesMap.keySet();
  }

  public Map<String, KeyDefinitionValue> getValuesWithDefinition() {
    return valuesMap;
  }
}
