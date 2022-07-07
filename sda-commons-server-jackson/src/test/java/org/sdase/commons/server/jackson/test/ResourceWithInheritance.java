package org.sdase.commons.server.jackson.test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true)
@JsonSubTypes({@JsonSubTypes.Type(value = ResourceWithInheritanceSubType.class, name = "SubType")})
public abstract class ResourceWithInheritance {
  private String type;

  public String getType() {
    return type;
  }

  public ResourceWithInheritance setType(String type) {
    this.type = type;
    return this;
  }
}
