package org.sdase.commons.shared.asyncapi.test.data.models;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(
    use = Id.NAME,
    property = "engineType",
    visible = true,
    include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
  @Type(value = Electrical.class, name = "ELECTRICAL"),
  @Type(value = Combustion.class, name = "COMBUSTION"),
})
@SuppressWarnings("unused")
public class CarModel {

  @Schema(description = "The name of the car model", example = "Tesla Roadster")
  private String name;

  @JsonPropertyDescription("The type of engine")
  private String engineType;

  public String getName() {
    return name;
  }

  public CarModel setName(String name) {
    this.name = name;
    return this;
  }

  public String getEngineType() {
    return engineType;
  }

  public CarModel setEngineType(String engineType) {
    this.engineType = engineType;
    return this;
  }
}
