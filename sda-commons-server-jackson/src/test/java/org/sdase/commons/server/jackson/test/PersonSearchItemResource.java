package org.sdase.commons.server.jackson.test;

import java.util.HashMap;
import java.util.Map;
import org.sdase.commons.server.jackson.EnableFieldFilter;

@EnableFieldFilter
public class PersonSearchItemResource {

  private String name;

  private Map<String, String> customMap = new HashMap<>();

  public String getName() {
    return name;
  }

  public PersonSearchItemResource setName(String name) {
    this.name = name;
    return this;
  }

  public Map<String, String> getCustomMap() {
    return customMap;
  }

  public PersonSearchItemResource setCustomMap(Map<String, String> customMap) {
    this.customMap = customMap;
    return this;
  }
}
