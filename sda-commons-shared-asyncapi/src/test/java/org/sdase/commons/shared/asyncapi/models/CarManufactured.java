package org.sdase.commons.shared.asyncapi.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaExamples;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle;
import java.time.Instant;
import javax.validation.constraints.NotNull;

@JsonSchemaTitle("Car manufactured")
@JsonSchemaDescription("A new car was manufactured")
public class CarManufactured extends BaseEvent {

  @JsonProperty(required = true)
  @JsonPropertyDescription("The registration of the vehicle")
  @JsonSchemaExamples(value = {"BB324A81", "BFCB7DF1"})
  private String vehicleRegistration;

  @NotNull
  @JsonPropertyDescription("The time of manufacturing")
  private Instant date;

  @NotNull
  @JsonPropertyDescription("The model of the car")
  private CarModel model;

  public String getVehicleRegistration() {
    return vehicleRegistration;
  }

  public CarManufactured setVehicleRegistration(String vehicleRegistration) {
    this.vehicleRegistration = vehicleRegistration;
    return this;
  }

  public Instant getDate() {
    return date;
  }

  public CarManufactured setDate(Instant date) {
    this.date = date;
    return this;
  }

  public CarModel getModel() {
    return model;
  }

  public CarManufactured setModel(CarModel model) {
    this.model = model;
    return this;
  }
}
