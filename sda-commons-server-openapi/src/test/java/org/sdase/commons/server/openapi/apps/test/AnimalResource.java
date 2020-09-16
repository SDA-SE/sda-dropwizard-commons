package org.sdase.commons.server.openapi.apps.test;

import io.openapitools.jackson.dataformat.hal.HALLink;
import io.openapitools.jackson.dataformat.hal.annotation.Link;
import io.openapitools.jackson.dataformat.hal.annotation.Resource;
import io.swagger.v3.oas.annotations.media.Schema;

@Resource
@Schema(name = "Animal")
public class AnimalResource {
  @Link
  @Schema(description = "Link relation 'self': The HAL link referencing this file.")
  private HALLink self;

  @Schema(description = "Name of the animal", example = "Hasso")
  private String name;

  public HALLink getSelf() {
    return self;
  }

  public String getName() {
    return name;
  }
}
