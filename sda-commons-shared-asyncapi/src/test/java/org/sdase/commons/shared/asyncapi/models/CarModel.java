package org.sdase.commons.shared.asyncapi.models;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import org.sdase.commons.shared.asyncapi.schema.JsonSchemaExamples;

@JsonTypeInfo(
    use = Id.NAME,
    property = "engineType",
    visible = true,
    include = As.EXISTING_PROPERTY)
@JsonSubTypes({
  @Type(value = Electrical.class, name = "ELECTRICAL"),
  @Type(value = Combustion.class, name = "COMBUSTION"),
})
public class CarModel {

  @JsonPropertyDescription("The name of the car model")
  @JsonSchemaExamples(value = {"Tesla Roadster", "Hummer H1"})
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
