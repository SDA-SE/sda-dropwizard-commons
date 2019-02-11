package org.sdase.commons.server.auth.testing;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.auth.service.JwtAuthorizer;
import org.sdase.commons.server.auth.testing.test.AuthTestApp;
import org.sdase.commons.server.auth.testing.test.AuthTestConfig;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class AuthRuleIT {

   private static final DropwizardAppRule<AuthTestConfig> DW = new DropwizardAppRule<>(
         AuthTestApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

   private static final AuthRule AUTH = AuthRule.builder().build();

   @ClassRule
   public static final RuleChain CHAIN = RuleChain.outerRule(AUTH).around(DW);

   @Test
   public void shouldAccessOpenEndPointWithoutToken() {
      Response response = createWebTarget()
            .path("/open")
            .request(APPLICATION_JSON)
            .get();

      assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
      assertThat(response.readEntity(String.class)).isEqualTo("We are open.");
   }

   @Test
   public void shouldNotAccessSecureEndPointWithoutToken() {
      Response response = createWebTarget()
            .path("/secure") // NOSONAR
            .request(APPLICATION_JSON)
            .get();

      assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
      assertThat(response.getHeaderString(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON);
      assertThat(response.readEntity(new GenericType<Map<String, Object>>() {}))
            .containsKeys("title", "invalidParams"); // NOSONAR
   }

   @Test
   public void shouldNotAccessSecureEndPointWithInvalidToken() {
      Response response = createWebTarget()
            .path("/secure") // NOSONAR
            .request(APPLICATION_JSON)
            .header(AUTHORIZATION, "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
            .get();

      assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
      assertThat(response.getHeaderString(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON);
      assertThat(response.readEntity(new GenericType<Map<String, Object>>() {}))
            .containsKeys("title", "invalidParams");
   }

   @Test
   public void shouldAccessSecureEndPointWithToken() {
      Response response = createWebTarget()
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
      Response response = createWebTarget()
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
      Response response = createWebTarget()
            .path("/admin")
            .request(APPLICATION_JSON)
            .headers(AUTH.auth().buildAuthHeader())
            .get();

      assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
      assertThat(response.getHeaderString(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON);
      assertThat(response.readEntity(new GenericType<Map<String, Object>>() {}))
            .containsKeys("title", "invalidParams");
   }


   @Test
   public void shouldNotAllowRoleBasedAccessWithoutToken() {
      Response response = createWebTarget()
            .path("/admin")
            .request(APPLICATION_JSON)
            .get();

      assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
      assertThat(response.getHeaderString(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON);
      assertThat(response.readEntity(new GenericType<Map<String, Object>>() {}))
            .containsKeys("title", "invalidParams");
   }

   private WebTarget createWebTarget() {
      return DW.client().target("http://localhost:" + DW.getLocalPort());
   }
}
