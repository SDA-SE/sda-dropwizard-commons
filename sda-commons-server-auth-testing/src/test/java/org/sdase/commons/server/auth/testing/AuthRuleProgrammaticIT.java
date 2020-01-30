package org.sdase.commons.server.auth.testing;

import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.auth.testing.test.AuthTestApp;
import org.sdase.commons.server.auth.testing.test.AuthTestConfig;
import org.sdase.commons.server.testing.DropwizardRuleHelper;

public class AuthRuleProgrammaticIT {

  private static AuthRule AUTH = AuthRule.builder().build();

  @ClassRule
  public static DropwizardAppRule<AuthTestConfig> DW =
      DropwizardRuleHelper.dropwizardTestAppFrom(AuthTestApp.class)
          .withConfigFrom(AuthTestConfig::new)
          .withRandomPorts()
          .withConfigurationModifier(AUTH.applyConfig(AuthTestConfig::setAuth))
          .build();

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
  public void shouldNotAccessSecureEndPointWithoutToken() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/secure")
            .request(APPLICATION_JSON)
            .get();

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Test
  public void shouldAccessSecureEndPointWithToken() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/secure")
            .request(APPLICATION_JSON)
            .headers(AUTH.auth().buildAuthHeader())
            .get();

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    assertThat(response.readEntity(new GenericType<Map<String, String>>() {}))
        .contains(entry("iss", "AuthRule"), entry("sub", "test"));
  }

  @Test
  public void shouldGetClaimsFromSecureEndPointWithToken() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/secure")
            .request(APPLICATION_JSON)
            .headers(
                AUTH.auth()
                    .addClaim("test", "testClaim")
                    .addClaims(singletonMap("mapKey", "testClaimFromMap"))
                    .buildAuthHeader())
            .get();

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    assertThat(response.readEntity(new GenericType<Map<String, String>>() {}))
        .contains(
            entry("iss", "AuthRule"),
            entry("sub", "test"),
            entry("test", "testClaim"),
            entry("mapKey", "testClaimFromMap"));
  }
}
