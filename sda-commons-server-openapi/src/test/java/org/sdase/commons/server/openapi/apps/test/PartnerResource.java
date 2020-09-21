package org.sdase.commons.server.openapi.apps.test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "Partner")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({@JsonSubTypes.Type(value = NaturalPersonResource.class, name = "naturalPerson")})
public abstract class PartnerResource {

  @Schema(
      description = "The type of partner, controls the available properties.",
      required = true,
      allowableValues = "naturalPerson",
      example = "naturalPerson")
  private String type;

  private List<String> options;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public List<String> getOptions() {
    return options;
  }

  public void setOptions(List<String> options) {
    this.options = options;
  }
}
