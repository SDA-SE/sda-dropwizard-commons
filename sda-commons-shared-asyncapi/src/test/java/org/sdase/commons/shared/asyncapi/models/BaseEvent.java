package org.sdase.commons.shared.asyncapi.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaExamples;

@JsonTypeInfo(
    use = Id.NAME,
    property = "type",
    visible = true,
    include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
  @Type(value = CarManufactured.class, name = "CAR_MANUFACTURED"),
  @Type(value = CarScrapped.class, name = "CAR_SCRAPPED"),
})
public abstract class BaseEvent {

  @JsonProperty(value = "id", required = true)
  @JsonPropertyDescription("The id of the message")
  @JsonSchemaExamples({
    "626A0F21-D940-4B44-BD36-23F0F567B0D0",
    "A6E6928D-EF92-4BE8-9DFA-76C935EF3446"
  })
  private String id;

  @JsonProperty("type")
  @JsonPropertyDescription("The type of message")
  private String type;
}
