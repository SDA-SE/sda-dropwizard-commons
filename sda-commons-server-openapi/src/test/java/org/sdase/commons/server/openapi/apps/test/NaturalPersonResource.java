package org.sdase.commons.server.openapi.apps.test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.openapitools.jackson.dataformat.hal.HALLink;
import io.openapitools.jackson.dataformat.hal.annotation.Link;
import io.openapitools.jackson.dataformat.hal.annotation.Resource;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;

@Resource
@Schema(name = "NaturalPerson")
public class NaturalPersonResource extends PartnerResource {
  @Link
  @Schema(description = "Link relation 'self': The HAL link referencing this file.")
  private HALLink self;

  @Schema(name = "firstName", example = "John")
  private final String firstName;

  @Schema(name = "lastName", example = "Doe")
  private final String lastName;

  @ArraySchema(arraySchema = @Schema(name = "traits", example = "[\"hipster\", \"generous\"]"))
  private final List<String> traits = new ArrayList<>();

  @JsonCreator
  public NaturalPersonResource(
      @JsonProperty("firstName") String firstName,
      @JsonProperty("lastName") String lastName,
      @JsonProperty("traits") List<String> traits,
      HALLink self) {

    this.firstName = firstName;
    this.lastName = lastName;
    this.traits.addAll(traits);
    this.self = self;
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

  public List<String> getTraits() {
    return traits;
  }
}
