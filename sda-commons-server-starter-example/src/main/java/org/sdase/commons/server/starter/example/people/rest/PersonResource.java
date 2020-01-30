package org.sdase.commons.server.starter.example.people.rest;

import io.openapitools.jackson.dataformat.hal.HALLink;
import io.openapitools.jackson.dataformat.hal.annotation.Link;
import io.openapitools.jackson.dataformat.hal.annotation.Resource;
import java.util.List;

/**
 * Example resource representing {@link
 * org.sdase.commons.server.starter.example.people.db.PersonEntity} in the REST API
 */
@Resource
public class PersonResource {

  @Link("self")
  private HALLink selfLink;

  @Link("children")
  private List<HALLink> childrenLinks;

  @Link("parents")
  private List<HALLink> parentsLinks;

  private String firstName;
  private String lastName;

  @SuppressWarnings("unused") // required for jackson
  public HALLink getSelfLink() {
    return selfLink;
  }

  PersonResource setSelfLink(HALLink selfLink) {
    this.selfLink = selfLink;
    return this;
  }

  @SuppressWarnings("unused") // required for jackson
  public String getFirstName() {
    return firstName;
  }

  PersonResource setFirstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  @SuppressWarnings("unused") // required for jackson
  public String getLastName() {
    return lastName;
  }

  PersonResource setLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  @SuppressWarnings("unused") // required for jackson
  public List<HALLink> getChildrenLinks() {
    return childrenLinks;
  }

  void setChildrenLinks(List<HALLink> childrenLinks) {
    this.childrenLinks = childrenLinks;
  }

  @SuppressWarnings("unused") // required for jackson
  public List<HALLink> getParentsLinks() {
    return parentsLinks;
  }

  void setParentsLinks(List<HALLink> parentsLinks) {
    this.parentsLinks = parentsLinks;
  }
}
