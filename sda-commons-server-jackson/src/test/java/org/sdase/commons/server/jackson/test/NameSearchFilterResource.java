package org.sdase.commons.server.jackson.test;

import javax.validation.constraints.NotNull;

public class NameSearchFilterResource extends SearchFilterResource {

  static final String TYPE = "NAME";

  @NotNull private String name;

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
}
