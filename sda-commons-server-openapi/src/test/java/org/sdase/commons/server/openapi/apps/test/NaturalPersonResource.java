package org.sdase.commons.server.openapi.apps.test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;

@Schema(name = "NaturalPerson")
public class NaturalPersonResource extends PartnerResource {
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
      @JsonProperty("traits") List<String> traits) {

    this.firstName = firstName;
    this.lastName = lastName;
    this.traits.addAll(traits);
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
