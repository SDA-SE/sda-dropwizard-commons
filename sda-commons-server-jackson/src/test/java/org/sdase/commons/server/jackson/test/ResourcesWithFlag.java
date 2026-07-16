package org.sdase.commons.server.jackson.test;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.openapitools.jackson.dataformat.hal.HALLink;
import io.openapitools.jackson.dataformat.hal.annotation.EmbeddedResource;
import io.openapitools.jackson.dataformat.hal.annotation.Link;
import io.openapitools.jackson.dataformat.hal.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;
import org.sdase.commons.server.jackson.EnableFieldFilter;

@Resource
@EnableFieldFilter(enableNestedPathFiltering = true)
class AddressResourceWithFlag {

  private String id;
  private String city;

  public String getCity() {
    return city;
  }

  public AddressResourceWithFlag setCity(String city) {
    this.city = city;
    return this;
  }

  public String getId() {
    return id;
  }

  public AddressResourceWithFlag setId(String id) {
    this.id = id;
    return this;
  }
}

@EnableFieldFilter(enableNestedPathFiltering = true)
class NestedNestedResourceWithFlag {

  @NotEmpty()
  @JsonProperty("anotherNestedField")
  private String anotherNested;

  @JsonProperty("someNumber")
  private int anotherNumber;

  public NestedNestedResourceWithFlag setAnotherNested(String anotherNested) {
    this.anotherNested = anotherNested;
    return this;
  }
}

@EnableFieldFilter(enableNestedPathFiltering = true)
class NestedResourceWithFlag {

  @NotEmpty()
  @JsonProperty("myNestedField")
  private String nested;

  @JsonProperty("someNumber")
  private int number;

  @Valid
  @JsonProperty("myNestedResource")
  private NestedNestedResourceWithFlag anotherNestedResource;

  public NestedNestedResourceWithFlag getAnotherNestedResource() {
    return anotherNestedResource;
  }

  public NestedResourceWithFlag setAnotherNestedResource(
      NestedNestedResourceWithFlag anotherNestedResource) {
    this.anotherNestedResource = anotherNestedResource;
    return this;
  }
}

@Resource
@EnableFieldFilter(enableNestedPathFiltering = true)
@SuppressWarnings("WeakerAccess")
class PersonResourceWithFlag {

  @Link private HALLink self;
  private String firstName;
  private String lastName;
  private String nickName;

  @Link(value = "address")
  private List<HALLink> addressLink;

  @EmbeddedResource private List<AddressResourceWithFlag> address;

  public HALLink getSelf() {
    return self;
  }

  public PersonResourceWithFlag setSelf(HALLink self) {
    this.self = self;
    return this;
  }

  public String getFirstName() {
    return firstName;
  }

  public PersonResourceWithFlag setFirstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public String getLastName() {
    return lastName;
  }

  public PersonResourceWithFlag setLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  public String getNickName() {
    return nickName;
  }

  public PersonResourceWithFlag setNickName(String nickName) {
    this.nickName = nickName;
    return this;
  }

  public List<HALLink> getAddressLink() {
    return addressLink;
  }

  public PersonResourceWithFlag setAddressLink(List<HALLink> addressLink) {
    this.addressLink = addressLink;
    return this;
  }

  public List<AddressResourceWithFlag> getAddress() {
    return address;
  }

  public PersonResourceWithFlag setAddress(List<AddressResourceWithFlag> address) {
    this.address = address;
    return this;
  }
}

@Resource
@EnableFieldFilter(enableNestedPathFiltering = true)
@SuppressWarnings("WeakerAccess")
class PersonWithChildrenResourceWithFlag {

  @Link private HALLink self;
  private String firstName;
  private String lastName;
  private String nickName;
  private AddressResourceWithFlag address;

  @JsonProperty("renamedCustomProp")
  private NestedResourceWithFlag nestedResource;

  private UnfilteredChildResource unfilteredChild;
  private List<UnfilteredChildResource> unfilteredChildren;
  private List<PersonResourceWithFlag> children;

  public HALLink getSelf() {
    return self;
  }

  public PersonWithChildrenResourceWithFlag setSelf(HALLink self) {
    this.self = self;
    return this;
  }

  public String getFirstName() {
    return firstName;
  }

  public PersonWithChildrenResourceWithFlag setFirstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public String getLastName() {
    return lastName;
  }

  public PersonWithChildrenResourceWithFlag setLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  public String getNickName() {
    return nickName;
  }

  public PersonWithChildrenResourceWithFlag setNickName(String nickName) {
    this.nickName = nickName;
    return this;
  }

  public AddressResourceWithFlag getAddress() {
    return address;
  }

  public PersonWithChildrenResourceWithFlag setAddress(AddressResourceWithFlag address) {
    this.address = address;
    return this;
  }

  public NestedResourceWithFlag getNestedResource() {
    return nestedResource;
  }

  public PersonWithChildrenResourceWithFlag setNestedResource(
      NestedResourceWithFlag nestedResource) {
    this.nestedResource = nestedResource;
    return this;
  }

  public UnfilteredChildResource getUnfilteredChild() {
    return unfilteredChild;
  }

  public PersonWithChildrenResourceWithFlag setUnfilteredChild(
      UnfilteredChildResource unfilteredChild) {
    this.unfilteredChild = unfilteredChild;
    return this;
  }

  public List<UnfilteredChildResource> getUnfilteredChildren() {
    return unfilteredChildren;
  }

  public PersonWithChildrenResourceWithFlag setUnfilteredChildren(
      List<UnfilteredChildResource> unfilteredChildren) {
    this.unfilteredChildren = unfilteredChildren;
    return this;
  }

  public List<PersonResourceWithFlag> getChildren() {
    return children;
  }

  public PersonWithChildrenResourceWithFlag setChildren(List<PersonResourceWithFlag> children) {
    this.children = children;
    return this;
  }
}

@EnableFieldFilter(enableNestedPathFiltering = true)
class MapChildResourceWithFlag {

  private String name;
  private String description;

  public String getName() {
    return name;
  }

  public MapChildResourceWithFlag setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public MapChildResourceWithFlag setDescription(String description) {
    this.description = description;
    return this;
  }
}

@EnableFieldFilter(enableNestedPathFiltering = true)
class MapContainerResourceWithFlag {

  private String id;
  private Map<String, MapChildResourceWithFlag> attributes;

  public String getId() {
    return id;
  }

  public MapContainerResourceWithFlag setId(String id) {
    this.id = id;
    return this;
  }

  public Map<String, MapChildResourceWithFlag> getAttributes() {
    return attributes;
  }

  public MapContainerResourceWithFlag setAttributes(
      Map<String, MapChildResourceWithFlag> attributes) {
    this.attributes = attributes;
    return this;
  }
}
