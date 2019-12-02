package org.sdase.commons.server.jackson.test;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;




public class NestedResource {

   @NotEmpty()
   @JsonProperty("myNestedField")
   private String nested;

   @JsonProperty("someNumber")
   private int number;

}
