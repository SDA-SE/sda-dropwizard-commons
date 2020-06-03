package org.sdase.commons.shared.asyncapi.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaExamples;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle;
import java.time.Instant;
import javax.validation.constraints.NotNull;

@JsonSchemaTitle("Car scrapped")
@JsonSchemaDescription("A car was scrapped")
public class CarScrapped extends BaseEvent {

  @JsonProperty(required = true)
  @JsonPropertyDescription("The registration of the vehicle")
  @JsonSchemaExamples(value = {"BB324A81", "BFCB7DF1"})
  private String vehicleRegistration;

  @NotNull
  @JsonPropertyDescription("The time of scrapping")
  private Instant date;

  @JsonPropertyDescription("The location where the car was scrapped")
  @JsonSchemaExamples(value = {"Hamburg"})
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
