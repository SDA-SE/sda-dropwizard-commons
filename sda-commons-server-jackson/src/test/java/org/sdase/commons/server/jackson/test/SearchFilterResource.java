package org.sdase.commons.server.jackson.test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = NameSearchFilterResource.class, name = NameSearchFilterResource.TYPE)
})
public abstract class SearchFilterResource {

  private String type;

  protected SearchFilterResource(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
