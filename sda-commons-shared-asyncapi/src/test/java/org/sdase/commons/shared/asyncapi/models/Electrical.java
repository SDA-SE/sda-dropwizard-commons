package org.sdase.commons.shared.asyncapi.models;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaExamples;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle;
import javax.validation.constraints.NotNull;

@JsonSchemaTitle("Electrical engine")
@JsonSchemaDescription("An car model with an electrical engine")
public class Electrical extends CarModel {

  @JsonPropertyDescription("The capacity of the battery in kwH")
  @JsonSchemaExamples(value = {"200"})
  @NotNull
  private int batteryCapacity;

  public int getBatteryCapacity() {
    return batteryCapacity;
  }

  public Electrical setBatteryCapacity(int batteryCapacity) {
    this.batteryCapacity = batteryCapacity;
    return this;
  }
}
