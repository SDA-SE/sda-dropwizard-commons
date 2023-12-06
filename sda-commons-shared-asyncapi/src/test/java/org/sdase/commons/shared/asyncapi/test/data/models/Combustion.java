package org.sdase.commons.shared.asyncapi.test.data.models;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(title = "Combustion engine", description = "An car model with a combustion engine")
@SuppressWarnings("unused")
public class Combustion extends CarModel {

  @NotNull
  @Schema(description = "The capacity of the tank in liter", example = "95")
  private int tankVolume;

  public int getTankVolume() {
    return tankVolume;
  }

  public Combustion setTankVolume(int tankVolume) {
    this.tankVolume = tankVolume;
    return this;
  }
}
