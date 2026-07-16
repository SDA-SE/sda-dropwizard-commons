package org.sdase.commons.server.jackson.test.snippets;

import io.openapitools.jackson.dataformat.hal.HALLink;
import io.openapitools.jackson.dataformat.hal.annotation.Link;
import java.util.List;
import java.util.Map;
import org.sdase.commons.server.jackson.EnableFieldFilter;

@EnableFieldFilter(enableNestedPathFiltering = true)
class Person {

  @Link private HALLink self;
  private String firstName;
  private String lastName;
  private String nickName;
  private Address address;
  private List<Person> children;
  private List<UnfilteredListChild> unfilteredChildren;
  private Map<String, Attribute> attributes;
  private UnfilteredChild unfilteredChild;
}

@EnableFieldFilter(enableNestedPathFiltering = true)
class Address {

  private String id;
  private String city;
  private String country;
}

@EnableFieldFilter(enableNestedPathFiltering = true)
class Attribute {

  private String name;
  private String description;
}

class UnfilteredChild {

  private String name;
  private String lastName;
}

class UnfilteredListChild {

  private String name;
  private String lastName;
}
