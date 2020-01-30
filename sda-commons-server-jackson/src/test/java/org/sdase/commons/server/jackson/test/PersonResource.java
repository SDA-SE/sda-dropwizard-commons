package org.sdase.commons.server.jackson.test;

import io.openapitools.jackson.dataformat.hal.HALLink;
import io.openapitools.jackson.dataformat.hal.annotation.EmbeddedResource;
import io.openapitools.jackson.dataformat.hal.annotation.Link;
import io.openapitools.jackson.dataformat.hal.annotation.Resource;
import java.util.List;
import org.sdase.commons.server.jackson.EnableFieldFilter;

@Resource
@EnableFieldFilter
@SuppressWarnings("WeakerAccess")
public class PersonResource {

  @Link private HALLink self;

  private String firstName;

  private String lastName;

  private String nickName;

  @Link(value = "address")
  private List<HALLink> addressLink;

  @EmbeddedResource private List<Address> address;

  public HALLink getSelf() {
    return self;
  }

  public PersonResource setSelf(HALLink self) {
    this.self = self;
    return this;
  }

  public String getFirstName() {
    return firstName;
  }

  public PersonResource setFirstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public String getLastName() {
    return lastName;
  }

  public PersonResource setLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  public String getNickName() {
    return nickName;
  }

  public PersonResource setNickName(String nickName) {
    this.nickName = nickName;
    return this;
  }

  public List<HALLink> getAddressLink() {
    return addressLink;
  }

  public PersonResource setAddressLink(List<HALLink> addressLink) {
    this.addressLink = addressLink;
    return this;
  }

  public List<Address> getAddress() {
    return address;
  }

  public PersonResource setAddress(List<Address> address) {
    this.address = address;
    return this;
  }
}
