package org.sdase.commons.server.jackson.test;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class NameSearchFilterResource extends SearchFilterResource {

  static final String TYPE = "NAME";

  @NotNull private String name;

  @Valid private NestedResource nestedResource;

  public NameSearchFilterResource() {
    super(TYPE);
  }

  public String getName() {
    return name;
  }

  public NameSearchFilterResource setName(String name) {
    this.name = name;
    return this;
  }

  public NestedResource getNestedResource() {
    return nestedResource;
  }

  public NameSearchFilterResource setNestedResource(NestedResource nestedResource) {
    this.nestedResource = nestedResource;
    return this;
  }
}
