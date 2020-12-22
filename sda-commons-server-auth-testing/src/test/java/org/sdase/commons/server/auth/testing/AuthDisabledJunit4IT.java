package org.sdase.commons.server.auth.testing;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.auth.testing.test.AuthTestApp;
import org.sdase.commons.server.auth.testing.test.AuthTestConfig;

public class AuthDisabledJunit4IT {

  private static DropwizardAppRule<AuthTestConfig> DW =
      new DropwizardAppRule<>(
          AuthTestApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

  private static AuthRule AUTH = AuthRule.builder().withDisabledAuth().build();

  @ClassRule public static RuleChain CHAIN = RuleChain.outerRule(AUTH).around(DW);

  @Test
  public void shouldAccessOpenEndPointWithoutToken() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/open")
            .request(APPLICATION_JSON)
            .get();

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    assertThat(response.readEntity(String.class)).isEqualTo("We are open.");
  }

  @Test
  public void shouldAccessSecureEndPointWithoutToken() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/secure")
            .request(APPLICATION_JSON)
            .get();

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowExceptionIfRequestingTokenWhileAuthIsDisabled() {
    AUTH.auth().buildToken();
  }
}
