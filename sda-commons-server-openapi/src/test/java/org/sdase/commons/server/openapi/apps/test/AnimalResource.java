package org.sdase.commons.server.openapi.apps.test;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Animal")
public class AnimalResource {
  @Schema(description = "Name of the animal", example = "Hasso")
  private String name;

  public String getName() {
    return name;
  }
}
