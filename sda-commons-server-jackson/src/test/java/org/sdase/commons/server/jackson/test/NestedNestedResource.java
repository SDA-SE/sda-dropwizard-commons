package org.sdase.commons.server.jackson.test;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotEmpty;

public class NestedNestedResource {

  @NotEmpty()
  @JsonProperty("anotherNestedField")
  private String anotherNested;

  @JsonProperty("someNumber")
  private int anotherNumber;
}
