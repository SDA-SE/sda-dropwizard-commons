package org.sdase.commons.server.opa.testing;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.Collections;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.auth.testing.AuthRule;
import org.sdase.commons.server.opa.testing.test.OpaJwtPrincipalInjectApp;

public class OpaJwtPrincipalInjectIT {

  private static final AuthRule AUTH = AuthRule.builder().build();
  private static final OpaRule OPA = new OpaRule();

  private static final DropwizardAppRule<OpaJwtPrincipalInjectApp.Config> DW =
      new DropwizardAppRule<>(
          OpaJwtPrincipalInjectApp.class,
          resourceFilePath("test-config.yaml"),
          config("opa.baseUrl", OPA::getUrl));

  @ClassRule public static RuleChain CHAIN = RuleChain.outerRule(AUTH).around(OPA).around(DW);

  @Before
  public void setUp() {
    OPA.reset();
  }

  @Test
  public void shouldInjectPrincipalWithConstraints() {

    String token = AUTH.auth().buildToken();
    OPA.mock(
        OpaRule.onAnyRequest()
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
  public void shouldProvideConstraintsWithoutUserContext() {

    OPA.mock(
        OpaRule.onAnyRequest()
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
  public void shouldRejectWithForbiddenWithoutUserContext() {

    OPA.mock(OpaRule.onAnyRequest().deny());

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
  public void shouldCreateSeparateContextForEachRequest() {

    String token1 = AUTH.auth().addClaim("foo", "bar").buildToken();
    String token2 = AUTH.auth().addClaim("bar", "foo").buildToken();
    OPA.mock(
        OpaRule.onAnyRequest()
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
