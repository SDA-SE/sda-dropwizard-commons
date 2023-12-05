/*
 * Copyright 2022- SDA SE Open Industry Solutions (https://www.sda.se)
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.sdase.commons.shared.asyncapi.test.data.models;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@JsonClassDescription("A car was scrapped")
@SuppressWarnings("unused")
public class CarScrapped extends BaseEvent {

  @NotBlank
  @JsonPropertyDescription("The registration of the vehicle")
  @Schema(example = "BB324A81")
  private String vehicleRegistration;

  @NotNull
  @JsonPropertyDescription("The time of scrapping")
  private Instant date;

  @JsonPropertyDescription("The location where the car was scrapped")
  private String location;

  public String getVehicleRegistration() {
    return vehicleRegistration;
  }

  public CarScrapped setVehicleRegistration(String vehicleRegistration) {
    this.vehicleRegistration = vehicleRegistration;
    return this;
  }

  public Instant getDate() {
    return date;
  }

  public CarScrapped setDate(Instant date) {
    this.date = date;
    return this;
  }

  public String getLocation() {
    return location;
  }

  public CarScrapped setLocation(String location) {
    this.location = location;
    return this;
  }
}
