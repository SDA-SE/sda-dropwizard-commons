package org.sdase.commons.server.opa.testing;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.sdase.commons.server.opa.testing.AbstractOpa.onAnyRequest;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.Collections;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.RetryingTest;
import org.sdase.commons.server.auth.testing.AuthClassExtension;
import org.sdase.commons.server.opa.testing.test.OpaJwtPrincipalInjectApp;

class OpaJwtPrincipalInjectIT {

  @RegisterExtension
  @Order(0)
  private static final AuthClassExtension AUTH = AuthClassExtension.builder().build();

  @RegisterExtension
  @Order(1)
  private static final OpaClassExtension OPA = new OpaClassExtension();

  @RegisterExtension
  @Order(2)
  private static final DropwizardAppExtension<OpaJwtPrincipalInjectApp.Config> DW =
      new DropwizardAppExtension<>(
          OpaJwtPrincipalInjectApp.class,
          resourceFilePath("test-config.yaml"),
          config("opa.baseUrl", OPA::getUrl));

  @BeforeEach
  void setUp() {
    OPA.reset();
  }

  @Test
  @RetryingTest(5)
  void shouldInjectPrincipalWithConstraints() {

    String token = AUTH.auth().buildToken();
    OPA.mock(
        onAnyRequest()
            .allow()
            .withConstraint(Collections.singletonMap("allowedOwners", new String[] {"ownerId"})));

    OpaJwtPrincipalInjectApp.Constraints constraints =
        client()
            .path("principal")
            .path("constraints")
            .request(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .get(OpaJwtPrincipalInjectApp.Constraints.class);

    assertThat(constraints).isNotNull();
    assertThat(constraints.getAllowedOwners()).contains("ownerId");
    assertThat(constraints.isAllow()).isTrue();
  }

  @Test
  @RetryingTest(5)
  void shouldProvideConstraintsWithoutUserContext() {

    OPA.mock(
        onAnyRequest()
            .allow()
            .withConstraint(Collections.singletonMap("allowedOwners", new String[] {})));

    OpaJwtPrincipalInjectApp.Constraints constraints =
        client()
            .path("principal")
            .path("constraints")
            .request(MediaType.APPLICATION_JSON)
            .get(OpaJwtPrincipalInjectApp.Constraints.class);

    assertThat(constraints).isNotNull();
    assertThat(constraints.getAllowedOwners()).isEmpty();
    assertThat(constraints.isAllow()).isTrue();
  }

  @Test
  @RetryingTest(5)
  void shouldRejectWithForbiddenWithoutUserContext() {

    OPA.mock(onAnyRequest().deny());

    assertThatExceptionOfType(ForbiddenException.class)
        .isThrownBy(
            () ->
                client()
                    .path("principal")
                    .path("constraints")
                    .request(MediaType.APPLICATION_JSON)
                    .get(OpaJwtPrincipalInjectApp.Constraints.class));
  }

  @Test
  @RetryingTest(5)
  void shouldCreateSeparateContextForEachRequest() {

    String token1 = AUTH.auth().addClaim("foo", "bar").buildToken();
    String token2 = AUTH.auth().addClaim("bar", "foo").buildToken();
    OPA.mock(
        onAnyRequest()
            .allow()
            .withConstraint(Collections.singletonMap("allowedOwners", new String[] {"ownerId"})));

    String actualToken1 =
        client()
            .path("principal")
            .path("token")
            .request(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token1)
            .get(String.class);

    String actualToken2 =
        client()
            .path("principal")
            .path("token")
            .request(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token2)
            .get(String.class);

    assertThat(actualToken1).isEqualTo(token1);
    assertThat(actualToken2).isEqualTo(token2);
    assertThat(token1).isNotEqualTo(token2);
  }

  private WebTarget client() {
    return DW.client().target("http://localhost:" + DW.getLocalPort());
  }
}
