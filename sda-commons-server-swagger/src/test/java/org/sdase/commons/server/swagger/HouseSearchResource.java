package org.sdase.commons.server.swagger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;

@ApiModel("HouseSearchResource")
public class HouseSearchResource {

  @ApiModelProperty(value = "A list of found houses", required = true)
  private List<HouseResource> houses = new ArrayList<>();

  @ApiModelProperty(value = "The total count of houses", required = true)
  private int totalCount;

  @JsonCreator
  public HouseSearchResource(
      @JsonProperty("houses") List<HouseResource> houses,
      @JsonProperty("totalCount") int totalCount) {

    this.houses.addAll(houses);
    this.totalCount = totalCount;
  }

  public List<HouseResource> getHouses() {
    return houses;
  }

  public int getTotalCount() {
    return totalCount;
  }
}
