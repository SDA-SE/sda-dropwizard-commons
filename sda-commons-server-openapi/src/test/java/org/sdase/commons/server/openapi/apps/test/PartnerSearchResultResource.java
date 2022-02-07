package org.sdase.commons.server.openapi.apps.test;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.ZonedDateTime;
import java.util.List;

public class PartnerSearchResultResource {

  @Schema(
      required = true,
      description =
          "The limiting timestamp until the consent has been selected by a user to be "
              + "included in the search result.",
      example = "2020-06-08T15:26:55Z")
  private ZonedDateTime timestamp;

  @Schema(
      required = true,
      description = "The number of total partner found for the request",
      example = "123")
  private long totalResults;

  @Schema(description = "The list of all partners based on the query request.", required = true)
  private List<String> partners;

  public ZonedDateTime getTimestamp() {
    return timestamp;
  }

  public PartnerSearchResultResource setTimestamp(ZonedDateTime timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public long getTotalResults() {
    return totalResults;
  }

  public PartnerSearchResultResource setTotalResults(long totalResults) {
    this.totalResults = totalResults;
    return this;
  }

  public List<String> getPartners() {
    return partners;
  }

  public PartnerSearchResultResource setPartners(List<String> partners) {
    this.partners = partners;
    return this;
  }
}
