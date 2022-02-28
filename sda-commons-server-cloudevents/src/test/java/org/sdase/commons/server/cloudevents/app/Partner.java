package org.sdase.commons.server.cloudevents.app;

public class Partner {

  private String id;
  private String firstName;
  private String lastName;

  public String getId() {
    return id;
  }

  public Partner setId(String id) {
    this.id = id;
    return this;
  }

  public String getFirstName() {
    return firstName;
  }

  public Partner setFirstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public String getLastName() {
    return lastName;
  }

  public Partner setLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }
}
