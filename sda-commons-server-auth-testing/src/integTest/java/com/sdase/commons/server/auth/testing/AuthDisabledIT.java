package com.sdase.commons.server.auth.testing;

import com.sdase.commons.server.auth.testing.test.AuthTestApp;
import com.sdase.commons.server.auth.testing.test.AuthTestConfig;
import com.sdase.commons.server.testing.EnvironmentRule;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

public class AuthDisabledIT {

   private static DropwizardAppRule<AuthTestConfig> DW = new DropwizardAppRule<>(
         AuthTestApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

   private static EnvironmentRule ENV = new EnvironmentRule().setEnv("AUTH_RULE", "{\"disableAuth\": true}");

   @ClassRule
   public static RuleChain CHAIN = RuleChain.outerRule(ENV).around(DW);

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
   public void shouldAccessSecureEndPointWithoutToken() {
      Response response = DW.client().target("http://localhost:" + DW.getLocalPort())
            .path("/secure")
            .request(APPLICATION_JSON)
            .get();

      assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
   }

   @Test
   public void shouldAllowAdminAccessIfAuthIsDisabled() {
      Response response = DW.client().target("http://localhost:" + DW.getLocalPort())
            .path("/admin")
            .request(APPLICATION_JSON)
            .get();

      assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
   }
}
