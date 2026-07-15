package org.sdase.commons.server.jackson.test;

import java.util.Map;
import org.sdase.commons.server.jackson.EnableFieldFilter;

@EnableFieldFilter
class MapChildResource {

  private String name;
  private String description;

  public String getName() {
    return name;
  }

  public MapChildResource setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public MapChildResource setDescription(String description) {
    this.description = description;
    return this;
  }
}

@EnableFieldFilter
class MapContainerResource {

  private String id;
  private Map<String, MapChildResource> attributes;

  public String getId() {
    return id;
  }

  public MapContainerResource setId(String id) {
    this.id = id;
    return this;
  }

  public Map<String, MapChildResource> getAttributes() {
    return attributes;
  }

  public MapContainerResource setAttributes(Map<String, MapChildResource> attributes) {
    this.attributes = attributes;
    return this;
  }
}
