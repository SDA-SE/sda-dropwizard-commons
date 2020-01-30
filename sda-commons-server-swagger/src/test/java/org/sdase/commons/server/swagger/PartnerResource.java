package org.sdase.commons.server.swagger;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("Partner")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({@JsonSubTypes.Type(value = NaturalPersonResource.class, name = "naturalPerson")})
public abstract class PartnerResource {
  @ApiModelProperty(
      value = "The type of partner, controls the available properties.",
      required = true,
      allowableValues = "naturalPerson",
      example = "naturalPerson")
  private String type;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
