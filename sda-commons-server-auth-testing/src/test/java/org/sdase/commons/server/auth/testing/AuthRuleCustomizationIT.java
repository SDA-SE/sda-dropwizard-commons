package org.sdase.commons.server.auth.testing;

import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.auth.testing.test.AuthTestApp;
import org.sdase.commons.server.auth.testing.test.AuthTestConfig;

public class AuthRuleCustomizationIT {

  private static final DropwizardAppRule<AuthTestConfig> DW =
      new DropwizardAppRule<>(
          AuthTestApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

  private static final AuthRule AUTH =
      AuthRule.builder()
          .withKeyId(null)
          .withIssuer("customIssuer") // NOSONAR
          .withSubject("customSubject") // NOSONAR
          .withCustomKeyPair(
              AuthRuleCustomizationIT.class.getResource("/test.pem").toString(),
              AuthRuleCustomizationIT.class.getResource("/test.key").toString())
          .build();

  @ClassRule public static final RuleChain CHAIN = RuleChain.outerRule(AUTH).around(DW);

  @Test
  public void shouldAccessOpenEndPointWithoutToken() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort()) // NOSONAR
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
            .path("/secure") // NOSONAR
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
        .contains(entry("iss", "customIssuer"), entry("sub", "customSubject"));
  }

  @Test
  public void shouldDenyAccessWhenTokenExpires() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/secure")
            .request(APPLICATION_JSON)
            .headers(
                AUTH.auth()
                    .addClaim("exp", new GregorianCalendar(1956, Calendar.MARCH, 17).getTime())
                    .buildAuthHeader())
            .get();

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    assertThat(response.getHeaderString("WWW-Authenticate")).contains("Bearer");
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
            entry("iss", "customIssuer"),
            entry("sub", "customSubject"),
            entry("test", "testClaim"),
            entry("mapKey", "testClaimFromMap"));
  }
}
