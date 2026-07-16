package org.sdase.commons.server.jackson.test.snippets;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.openapitools.jackson.dataformat.hal.HALLink;
import io.openapitools.jackson.dataformat.hal.annotation.Link;
import io.openapitools.jackson.dataformat.hal.annotation.Resource;
import java.util.List;
import java.util.Map;
import org.sdase.commons.server.jackson.EnableFieldFilter;

public final class EnableFieldFilterExampleModel {

  private EnableFieldFilterExampleModel() {}

  @Resource
  @EnableFieldFilter(enableNestedPathFiltering = true)
  public static class Person {

    @Link private HALLink self;
    private String id;
    private String firstName;
    private String lastName;
    private String nickName;
    private Address address;
    private List<Person> children;
    private List<UnfilteredChild> unfilteredChildren;
    private Map<String, Attribute> attributes;
    private UnfilteredChild unfilteredChild;

    @JsonProperty("renamedCustomProp")
    private NestedResource nestedResource;

    public HALLink getSelf() {
      return self;
    }

    public Person setSelf(HALLink self) {
      this.self = self;
      return this;
    }

    public String getId() {
      return id;
    }

    public Person setId(String id) {
      this.id = id;
      return this;
    }

    public String getFirstName() {
      return firstName;
    }

    public Person setFirstName(String firstName) {
      this.firstName = firstName;
      return this;
    }

    public String getLastName() {
      return lastName;
    }

    public Person setLastName(String lastName) {
      this.lastName = lastName;
      return this;
    }

    public String getNickName() {
      return nickName;
    }

    public Person setNickName(String nickName) {
      this.nickName = nickName;
      return this;
    }

    public Address getAddress() {
      return address;
    }

    public Person setAddress(Address address) {
      this.address = address;
      return this;
    }

    public List<Person> getChildren() {
      return children;
    }

    public Person setChildren(List<Person> children) {
      this.children = children;
      return this;
    }

    public List<UnfilteredChild> getUnfilteredChildren() {
      return unfilteredChildren;
    }

    public Person setUnfilteredChildren(List<UnfilteredChild> unfilteredChildren) {
      this.unfilteredChildren = unfilteredChildren;
      return this;
    }

    public Map<String, Attribute> getAttributes() {
      return attributes;
    }

    public Person setAttributes(Map<String, Attribute> attributes) {
      this.attributes = attributes;
      return this;
    }

    public UnfilteredChild getUnfilteredChild() {
      return unfilteredChild;
    }

    public Person setUnfilteredChild(UnfilteredChild unfilteredChild) {
      this.unfilteredChild = unfilteredChild;
      return this;
    }

    public NestedResource getNestedResource() {
      return nestedResource;
    }

    public Person setNestedResource(NestedResource nestedResource) {
      this.nestedResource = nestedResource;
      return this;
    }
  }

  @EnableFieldFilter(enableNestedPathFiltering = true)
  public static class Address {

    private String id;
    private String city;
    private String country;

    public String getId() {
      return id;
    }

    public Address setId(String id) {
      this.id = id;
      return this;
    }

    public String getCity() {
      return city;
    }

    public Address setCity(String city) {
      this.city = city;
      return this;
    }

    public String getCountry() {
      return country;
    }

    public Address setCountry(String country) {
      this.country = country;
      return this;
    }
  }

  @EnableFieldFilter(enableNestedPathFiltering = true)
  public static class Attribute {

    private String name;
    private String description;

    public String getName() {
      return name;
    }

    public Attribute setName(String name) {
      this.name = name;
      return this;
    }

    public String getDescription() {
      return description;
    }

    public Attribute setDescription(String description) {
      this.description = description;
      return this;
    }
  }

  public static class UnfilteredChild {

    @JsonProperty("name")
    private String firstName;

    private String lastName;

    public String getFirstName() {
      return firstName;
    }

    public UnfilteredChild setFirstName(String firstName) {
      this.firstName = firstName;
      return this;
    }

    public String getLastName() {
      return lastName;
    }

    public UnfilteredChild setLastName(String lastName) {
      this.lastName = lastName;
      return this;
    }
  }

  @EnableFieldFilter(enableNestedPathFiltering = true)
  public static class NestedResource {

    @JsonProperty("myNestedField")
    private String nested;

    @JsonProperty("someNumber")
    private int number;

    @JsonProperty("myNestedResource")
    private NestedNestedResource anotherNestedResource;

    public NestedNestedResource getAnotherNestedResource() {
      return anotherNestedResource;
    }

    public NestedResource setAnotherNestedResource(NestedNestedResource anotherNestedResource) {
      this.anotherNestedResource = anotherNestedResource;
      return this;
    }
  }

  @EnableFieldFilter(enableNestedPathFiltering = true)
  public static class NestedNestedResource {

    @JsonProperty("anotherNestedField")
    private String anotherNested;

    @JsonProperty("someNumber")
    private int anotherNumber;

    public NestedNestedResource setAnotherNested(String anotherNested) {
      this.anotherNested = anotherNested;
      return this;
    }
  }
}
