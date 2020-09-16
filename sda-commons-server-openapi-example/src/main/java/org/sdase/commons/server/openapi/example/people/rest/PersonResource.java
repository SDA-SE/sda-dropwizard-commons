package org.sdase.commons.server.openapi.example.people.rest;

import io.openapitools.jackson.dataformat.hal.HALLink;
import io.openapitools.jackson.dataformat.hal.annotation.Link;
import io.openapitools.jackson.dataformat.hal.annotation.Resource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Example resource representing a Person in the REST API */
@Resource
// Provide the description displayed in the swagger documentation.
@Schema(description = "Defines a person.")
public class PersonResource {

  @Link("self")
  // Provide the description of the self link property.
  @Schema(description = "Link relation 'self': The HAL link referencing this person.")
  private HALLink selfLink;

  // Provide the description and an example for the property. Examples are optional but help a
  // consumer to understand the behavior. Examples also have an effect on tools, like the Swagger UI
  // to display useful responds. Required allows to mark fields as non optional. Fields are optional
  // by default.
  @Schema(description = "The first name of the person.", required = true, example = "John")
  private String firstName;

  @Schema(description = "The last name of the person.", required = true, example = "Doe")
  private String lastName;

  // As long a the JSON example feature isn't disabled, one can use JSON to describe more complex
  // examples for sub values.
  @Schema(
      description = "The addresses of a person.",
      example =
          "[{\"street\":\"Reeperbahn 1\",\"city\":\"Hamburg\"},"
              + "{\"street\":\"Unter den Linden 5\",\"city\":\"Berlin\"}]")
  private List<AddressResource> addresses;

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
  public List<AddressResource> getAddresses() {
    return addresses;
  }

  public PersonResource setAddresses(List<AddressResource> addresses) {
    this.addresses = addresses;
    return this;
  }
}
