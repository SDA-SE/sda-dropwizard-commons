package org.sdase.commons.server.auth.testing;

import org.sdase.commons.server.auth.testing.test.AuthTestApp;
import org.sdase.commons.server.auth.testing.test.AuthTestConfig;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.auth.service.JwtAuthorizer;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class AuthRuleIT {

   private static DropwizardAppRule<AuthTestConfig> DW = new DropwizardAppRule<>(
         AuthTestApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

   private static AuthRule AUTH = AuthRule.builder().build();

   @ClassRule
   public static RuleChain CHAIN = RuleChain.outerRule(AUTH).around(DW);

   @Test
   public void shouldAccessOpenEndPointWithoutToken() {
      Response response = DW.client().target("http://localhost:" + DW.getLocalPort())
            .path("/open")
            .request(APPLICATION_JSON)
            .get();

      assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
      assertThat(response.readEntity(String.class)).isEqualTo("We are open.");
   }

   @Test
   public void shouldNotAccessSecureEndPointWithoutToken() {
      Response response = DW.client().target("http://localhost:" + DW.getLocalPort())
            .path("/secure")
            .request(APPLICATION_JSON)
            .get();

      assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
   }

   @Test
   public void shouldAccessSecureEndPointWithToken() {
      Response response = DW.client().target("http://localhost:" + DW.getLocalPort())
            .path("/secure")
            .request(APPLICATION_JSON)
            .headers(AUTH.auth().buildAuthHeader())
            .get();

      assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
      assertThat(response.readEntity(new GenericType<Map<String, String>>(){})).contains(
            entry("iss", "AuthRule"), entry("sub", "test")
      );
   }

   @Test
   public void shouldGetClaimsFromSecureEndPointWithToken() {
      Response response = DW.client().target("http://localhost:" + DW.getLocalPort())
            .path("/secure")
            .request(APPLICATION_JSON)
            .headers(AUTH.auth()
                  .addClaim("test", "testClaim")
                  .addClaims(singletonMap("mapKey", "testClaimFromMap"))
                  .buildAuthHeader())
            .get();

      assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
      assertThat(response.readEntity(new GenericType<Map<String, String>>(){}))
            .contains(
                  entry("iss", "AuthRule"),
                  entry("sub", "test"),
                  entry("test", "testClaim"),
                  entry("mapKey", "testClaimFromMap")
            );
   }

   /**
    * Role based authentication is not implemented yet (see {@link JwtAuthorizer})
    * We expect "403 Forbidden" until someone implements a useful role based system.
    */
   @Test
   public void shouldNotAllowRoleBasedAccessWithToken() {
      Response response = DW.client().target("http://localhost:" + DW.getLocalPort())
            .path("/admin")
            .request(APPLICATION_JSON)
            .headers(AUTH.auth().buildAuthHeader())
            .get();

      assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
   }


   @Test
   public void shouldNotAllowRoleBasedAccessWithoutToken() {
      Response response = DW.client().target("http://localhost:" + DW.getLocalPort())
            .path("/admin")
            .request(APPLICATION_JSON)
            .get();

      assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
   }

}
