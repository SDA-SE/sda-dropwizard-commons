package org.sdase.commons.server.swagger.example.people.rest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Defines a postal address.")
public class AddressResource {
  @ApiModelProperty(value = "The street of the address.", required = true, example = "Reeperbahn 1")
  private String street;

  @ApiModelProperty(value = "The city of the address.", required = true, example = "Hamburg")
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
