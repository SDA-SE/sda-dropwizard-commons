package org.sdase.commons.server.jackson.test;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import org.sdase.commons.server.jackson.EnableFieldFilter;

@EnableFieldFilter
public class NestedNestedResource {

  @NotEmpty()
  @JsonProperty("anotherNestedField")
  private String anotherNested;

  @JsonProperty("someNumber")
  private int anotherNumber;

  public NestedNestedResource setAnotherNested(String anotherNested) {
    this.anotherNested = anotherNested;
    return this;
  }
}
