package org.sdase.commons.shared.asyncapi.test.data.models;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@JsonTypeInfo(
    use = Id.NAME,
    property = "type",
    visible = true,
    include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
  @Type(value = CarManufactured.class, name = "CAR_MANUFACTURED"),
  @Type(value = CarScrapped.class, name = "CAR_SCRAPPED"),
})
@SuppressWarnings("unused")
public abstract class BaseEvent {

  @JsonPropertyDescription("The id of the message")
  @NotBlank
  @Schema(example = "626A0F21-D940-4B44-BD36-23F0F567B0D0")
  private String id;

  @JsonPropertyDescription("The type of message")
  private Type type;

  public String getId() {
    return id;
  }

  public BaseEvent setId(String id) {
    this.id = id;
    return this;
  }

  public Type getType() {
    return type;
  }

  public BaseEvent setType(Type type) {
    this.type = type;
    return this;
  }

  public enum Type {
    CAR_MANUFACTURED,
    CAR_SCRAPPED
  }
}
