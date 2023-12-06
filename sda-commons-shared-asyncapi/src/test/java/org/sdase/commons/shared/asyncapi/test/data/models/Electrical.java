package org.sdase.commons.shared.asyncapi.test.data.models;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(title = "Electrical engine", description = "An car model with an electrical engine")
@SuppressWarnings("unused")
public class Electrical extends CarModel {

  @NotNull
  @JsonPropertyDescription("The capacity of the battery in kwH")
  @Schema(example = "200")
  private int batteryCapacity;

  public int getBatteryCapacity() {
    return batteryCapacity;
  }

  public Electrical setBatteryCapacity(int batteryCapacity) {
    this.batteryCapacity = batteryCapacity;
    return this;
  }
}
