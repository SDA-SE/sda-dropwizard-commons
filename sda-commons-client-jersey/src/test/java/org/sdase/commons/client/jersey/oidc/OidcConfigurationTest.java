package org.sdase.commons.client.jersey.oidc;

import static org.assertj.core.api.Assertions.assertThat;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.junit.Before;
import org.junit.Test;

public class OidcConfigurationTest {

  private Validator validator;

  @Before
  public void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  public void shouldValidateDisabled() {
    assertThat(validator.validate(new OidcConfiguration().setDisabled(true))).isEmpty();
  }

  @Test
  public void shouldValidateMandatoryFields() {
    assertThat(validator.validate(new OidcConfiguration().setDisabled(false))).isNotEmpty();
  }

  @Test
  public void shouldValidateGrantTypeClientCredentials() {
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
  public void shouldValidateGrantTypePassword() {
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
