package org.sdase.commons.server.swagger;

import io.openapitools.jackson.dataformat.hal.HALLink;
import io.openapitools.jackson.dataformat.hal.annotation.Link;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.annotation.Resource;

@Resource
@ApiModel("Animal")
public class AnimalResource {
   @Link
   @ApiModelProperty("Link relation 'self': The HAL link referencing this file.")
   private HALLink self;

   @ApiModelProperty(value = "Name of the animal", example = "Hasso")
   private String name;

   public HALLink getSelf() {
      return self;
   }

   public String getName() {
      return name;
   }
}
