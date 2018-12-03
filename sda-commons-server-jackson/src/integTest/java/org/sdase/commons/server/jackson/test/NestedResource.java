package org.sdase.commons.server.jackson.test;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.validation.OneOf;
import io.openapitools.jackson.dataformat.hal.annotation.Resource;
import org.hibernate.validator.constraints.NotEmpty;
import org.sdase.commons.server.jackson.EnableFieldFilter;

import javax.validation.constraints.Pattern;



public class NestedResource {

   @NotEmpty()
   @JsonProperty("myNestedField")
   private String nested;

   private int number;

}
