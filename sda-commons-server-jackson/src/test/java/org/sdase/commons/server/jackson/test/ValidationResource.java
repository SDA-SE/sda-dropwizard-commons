package org.sdase.commons.server.jackson.test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.validation.OneOf;
import io.dropwizard.validation.ValidationMethod;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import org.hibernate.validator.constraints.NotEmpty;

@CheckNameRepeated
public class ValidationResource {

  @OneOf(
      value = {"m", "f"},
      ignoreCase = true,
      ignoreWhitespace = true)
  private String gender;

  @NotEmpty()
  @JsonProperty("name")
  private String firstName;

  @Pattern(regexp = "[A-Z][a-z]*")
  private String lastName;

  @Valid
  @JsonProperty("myNestedResource")
  private NestedResource nestedResource;

  @UpperCase private String salutation;

  public NestedResource getNestedResource() {
    return nestedResource;
  }

  public void setNestedResource(NestedResource nestedResource) {
    this.nestedResource = nestedResource;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public String getSalutation() {
    return salutation;
  }

  public void setSalutation(String salutation) {
    this.salutation = salutation;
  }

  @ValidationMethod(message = "Test")
  @JsonIgnore
  public boolean isValidationException() {
    if ("validationException".equals(firstName)) {
      throw new RuntimeException("Validation Failed RTE");
    } else {
      return true;
    }
  }

  @ValidationMethod(message = "name must not be 'Noname'")
  @JsonIgnore
  public boolean isNotNoname() {
    return !"Noname".equalsIgnoreCase(firstName);
  }
}
