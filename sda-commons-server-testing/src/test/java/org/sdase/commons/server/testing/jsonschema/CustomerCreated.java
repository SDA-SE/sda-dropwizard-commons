package org.sdase.commons.server.testing.jsonschema;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Schema(title = "Customer created", description = "Customer creation event")
public class CustomerCreated {
  @NotBlank
  @Schema(description = "Customer identifier", example = "customer-42")
  private String customerId;

  @NotNull private Instant occurredAt;

  public String getCustomerId() {
    return customerId;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}
