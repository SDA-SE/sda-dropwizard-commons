package org.sdase.commons.starter.example.people.db;

import java.util.ArrayList;
import java.util.List;

/** Example persistent entity holding information about a person. */
public class PersonEntity {

  private final String id;

  private final String firstName;
  private final String lastName;

  private final List<PersonEntity> children = new ArrayList<>();
  private final List<PersonEntity> parents = new ArrayList<>();

  PersonEntity(String id, String firstName, String lastName) {
    this.id = id;
    this.firstName = firstName;
    this.lastName = lastName;
  }

  public String getId() {
    return id;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public List<PersonEntity> getChildren() {
    return children;
  }

  public List<PersonEntity> getParents() {
    return parents;
  }
}
