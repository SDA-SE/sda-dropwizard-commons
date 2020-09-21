package org.sdase.commons.server.openapi.apps.test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;

@Schema(name = "House")
public class HouseResource {
  @ArraySchema(
      arraySchema = @Schema(description = "list of partners that live in the house (Embedded)"))
  private final List<PartnerResource> partners = new ArrayList<>();

  @ArraySchema(
      arraySchema = @Schema(description = "list of animals that live in the house (Embedded)"))
  private final List<AnimalResource> animals = new ArrayList<>();

  @JsonCreator
  public HouseResource(
      @JsonProperty("partners") List<PartnerResource> partners,
      @JsonProperty("animals") List<AnimalResource> animals) {

    this.partners.addAll(partners);
    this.animals.addAll(animals);
  }

  public List<PartnerResource> getPartners() {
    return partners;
  }

  public List<AnimalResource> getAnimals() {
    return animals;
  }
}
