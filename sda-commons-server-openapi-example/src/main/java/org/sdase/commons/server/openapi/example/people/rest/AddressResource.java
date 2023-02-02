package org.sdase.commons.server.openapi.example.people.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

@Schema(description = "Defines a postal address.")
public class AddressResource {
  @Schema(
      description = "The street of the address.",
      requiredMode = RequiredMode.REQUIRED,
      example = "Reeperbahn 1")
  private String street;

  @Schema(
      description = "The city of the address.",
      requiredMode = RequiredMode.REQUIRED,
      example = "Hamburg")
  private String city;

  public String getStreet() {
    return street;
  }

  public AddressResource setStreet(String street) {
    this.street = street;
    return this;
  }

  public String getCity() {
    return city;
  }

  public AddressResource setCity(String city) {
    this.city = city;
    return this;
  }
}
