package org.sdase.commons.starter.example.people.rest;

public class NewPersonResource {

  private String firstName;
  private String lastName;

  public String getFirstName() {
    return firstName;
  }

  public NewPersonResource setFirstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public String getLastName() {
    return lastName;
  }

  public NewPersonResource setLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }
}
