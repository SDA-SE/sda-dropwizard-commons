package org.sdase.commons.server.swagger.example.people.rest;

import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.auth.testing.AuthRule;
import org.sdase.commons.server.starter.SdaPlatformConfiguration;
import org.sdase.commons.server.swagger.example.SdaPlatformExampleApplication;
import org.sdase.commons.server.testing.DropwizardRuleHelper;

// This is a simple integration test that checks whether the swagger documentation is produced at
// the right path, however doesn't test the contents of the documentation.
public class SwaggerIT {

   // create a dummy authentication provider that works as a local OpenId
   // Connect provider for the tests
   private static final AuthRule AUTH = AuthRule.builder().build();

   @ClassRule
   public static final DropwizardAppRule<SdaPlatformConfiguration> DW =
         // Setup a test instance of the application
         DropwizardRuleHelper
               .dropwizardTestAppFrom(SdaPlatformExampleApplication.class)
               .withConfigFrom(SdaPlatformConfiguration::new)
               // use random ports so that tests can run in parallel
               // and do not affect each other when one is not shutting down
               .withRandomPorts()
               // apply the auth config to the test instance of the application
               // to verify incoming tokens correctly
               .withConfigurationModifier(AUTH.applyConfig(SdaPlatformConfiguration::setAuth))
               .build();

   @Test
   public void testAnswerSwaggerJson() {
      // given

      // when
      final Response r = baseUrlWebTarget().path("swagger.json").request().get();

      // then
      assertThat(r.getStatus()).isEqualTo(OK.getStatusCode());
   }

   @Test
   public void testAnswerSwaggerYaml() {
      // given

      // when
      final Response r = baseUrlWebTarget().path("swagger.yaml").request().get();

      // then
      assertThat(r.getStatus()).isEqualTo(OK.getStatusCode());
   }

   private WebTarget baseUrlWebTarget() {
      return DW.client().target(String.format("http://localhost:%d", DW.getLocalPort()));
   }
}
