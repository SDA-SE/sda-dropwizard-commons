package org.sdase.commons.server.swagger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.openapitools.jackson.dataformat.hal.HALLink;
import io.openapitools.jackson.dataformat.hal.annotation.Link;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.annotation.Resource;

@Resource
@ApiModel(description = "Person")
public class PersonResource {
   @Link
   @ApiModelProperty("Link relation 'self': The HAL link referencing this file.")
   private HALLink self;

   @ApiModelProperty("firstName")
   private final String firstName;

   @ApiModelProperty("lastName")
   private final String lastName;

   @JsonCreator
   public PersonResource(
         @JsonProperty("firstName") String firstName,
         @JsonProperty("lastName") String lastName, HALLink self) {

      this.firstName = firstName;
      this.lastName = lastName;
   }

   public HALLink getSelf() {
      return self;
   }

   public String getFirstName() {
      return firstName;
   }

   public String getLastName() {
      return lastName;
   }
}
