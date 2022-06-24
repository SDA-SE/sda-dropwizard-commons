package org.sdase.commons.server.jackson.test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = ResourceOne.class, name = "ONE"),
  @JsonSubTypes.Type(value = ResourceTwo.class, name = "TWO")
})
public abstract class AbstractResource {

  private ResourceType type;

  public ResourceType getType() {
    return type;
  }

  public AbstractResource setType(ResourceType type) {
    this.type = type;
    return this;
  }

  public enum ResourceType {
    ONE,
    TWO
  }
}
