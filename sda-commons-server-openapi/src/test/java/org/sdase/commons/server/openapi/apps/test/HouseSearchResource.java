package org.sdase.commons.server.openapi.apps.test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;

@Schema(name = "HouseSearchResource")
public class HouseSearchResource {

  @ArraySchema(arraySchema = @Schema(description = "The filters to apply", required = true))
  private List<AnimalFilter> filters = new ArrayList<>();

  @ArraySchema(arraySchema = @Schema(description = "A list of found houses", required = true))
  private List<HouseResource> houses = new ArrayList<>();

  @Schema(description = "The total count of houses", required = true)
  private int totalCount;

  @JsonCreator
  public HouseSearchResource(
      @JsonProperty("filters") List<AnimalFilter> filters,
      @JsonProperty("houses") List<HouseResource> houses,
      @JsonProperty("totalCount") int totalCount) {

    this.filters.addAll(filters);
    this.houses.addAll(houses);
    this.totalCount = totalCount;
  }

  public List<AnimalFilter> getFilters() {
    return filters;
  }

  public List<HouseResource> getHouses() {
    return houses;
  }

  public int getTotalCount() {
    return totalCount;
  }
}
