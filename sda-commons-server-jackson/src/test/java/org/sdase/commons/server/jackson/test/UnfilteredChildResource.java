package org.sdase.commons.server.jackson.test;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UnfilteredChildResource {

  @JsonProperty("name")
  private String firstName;

  private String lastName;

  public String getFirstName() {
    return firstName;
  }

  public UnfilteredChildResource setFirstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public String getLastName() {
    return lastName;
  }

  public UnfilteredChildResource setLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }
}
