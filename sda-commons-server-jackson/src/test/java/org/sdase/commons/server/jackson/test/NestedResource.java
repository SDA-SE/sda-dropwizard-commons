package org.sdase.commons.server.jackson.test;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

public class NestedResource {

  @NotEmpty()
  @JsonProperty("myNestedField")
  private String nested;

  @JsonProperty("someNumber")
  private int number;

  @Valid
  @JsonProperty("myNestedResource")
  private NestedNestedResource anotherNestedResource;

  public NestedNestedResource getAnotherNestedResource() {
    return anotherNestedResource;
  }

  public NestedResource setAnotherNestedResource(NestedNestedResource anotherNestedResource) {
    this.anotherNestedResource = anotherNestedResource;
    return this;
  }
}
