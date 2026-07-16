package org.sdase.commons.server.jackson.test;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.openapitools.jackson.dataformat.hal.HALLink;
import io.openapitools.jackson.dataformat.hal.annotation.Link;
import io.openapitools.jackson.dataformat.hal.annotation.Resource;
import java.util.List;
import org.sdase.commons.server.jackson.EnableFieldFilter;

@Resource
@EnableFieldFilter
@SuppressWarnings("WeakerAccess")
public class PersonWithChildrenResource {

  @Link private HALLink self;

  private String firstName;

  private String lastName;

  private String nickName;

  private Address address;

  @JsonProperty("renamedCustomProp")
  private NestedResource nestedResource;

  private UnfilteredChildResource unfilteredChild;

  private List<PersonResource> children;

  public HALLink getSelf() {
    return self;
  }

  public PersonWithChildrenResource setSelf(HALLink self) {
    this.self = self;
    return this;
  }

  public String getFirstName() {
    return firstName;
  }

  public PersonWithChildrenResource setFirstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public String getLastName() {
    return lastName;
  }

  public PersonWithChildrenResource setLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  public String getNickName() {
    return nickName;
  }

  public PersonWithChildrenResource setNickName(String nickName) {
    this.nickName = nickName;
    return this;
  }

  public Address getAddress() {
    return address;
  }

  public PersonWithChildrenResource setAddress(Address address) {
    this.address = address;
    return this;
  }

  public NestedResource getNestedResource() {
    return nestedResource;
  }

  public PersonWithChildrenResource setNestedResource(NestedResource nestedResource) {
    this.nestedResource = nestedResource;
    return this;
  }

  public UnfilteredChildResource getUnfilteredChild() {
    return unfilteredChild;
  }

  public PersonWithChildrenResource setUnfilteredChild(UnfilteredChildResource unfilteredChild) {
    this.unfilteredChild = unfilteredChild;
    return this;
  }

  public List<PersonResource> getChildren() {
    return children;
  }

  public PersonWithChildrenResource setChildren(List<PersonResource> children) {
    this.children = children;
    return this;
  }
}
