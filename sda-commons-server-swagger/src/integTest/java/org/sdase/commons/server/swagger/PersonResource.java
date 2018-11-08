package org.sdase.commons.server.swagger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.annotation.Resource;

@Resource
@ApiModel(description = "Person")
public class PersonResource {

   @ApiModelProperty("firstName")
   private final String firstName;

   @ApiModelProperty("lastName")
   private final String lastName;

   @JsonCreator
   public PersonResource(
         @JsonProperty("firstName") String firstName,
         @JsonProperty("lastName") String lastName) {

      this.firstName = firstName;
      this.lastName = lastName;
   }

   public String getFirstName() {
      return firstName;
   }

   public String getLastName() {
      return lastName;
   }
}
