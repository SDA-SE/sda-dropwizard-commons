package org.sdase.commons.client.jersey.oidc;

import static org.assertj.core.api.Assertions.assertThat;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OidcConfigurationTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void shouldValidateDisabled() {
    assertThat(validator.validate(new OidcConfiguration().setDisabled(true))).isEmpty();
  }

  @Test
  void shouldValidateMandatoryFields() {
    assertThat(validator.validate(new OidcConfiguration().setDisabled(false))).isNotEmpty();
  }

  @Test
  void shouldValidateGrantTypeClientCredentials() {
    assertThat(
            validator.validate(
                new OidcConfiguration()
                    .setDisabled(false)
                    .setIssuerUrl("asd")
                    .setGrantType("client_credentials")
                    .setClientId("clientid")
                    .setClientSecret("clientsecret")))
        .isEmpty();
  }

  @Test
  void shouldValidateGrantTypePassword() {
    assertThat(
            validator.validate(
                new OidcConfiguration()
                    .setDisabled(false)
                    .setIssuerUrl("asd")
                    .setGrantType("password")
                    .setClientId("clientid")
                    .setClientSecret("clientsecret")
                    .setUsername("user")
                    .setPassword("pass")))
        .isEmpty();
  }
}
