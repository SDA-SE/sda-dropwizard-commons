package org.sdase.commons.server.starter;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sdase.commons.server.testing.DropwizardRuleHelper.dropwizardTestAppFrom;

import java.util.Map;

import javax.ws.rs.core.GenericType;

import javax.ws.rs.core.Response;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.auth.config.AuthConfig;
import org.sdase.commons.server.cors.CorsConfiguration;
import org.sdase.commons.server.starter.test.StarterApp;

import io.dropwizard.testing.junit.DropwizardAppRule;

public class FilterPriorityTest {

   @ClassRule
   public static final DropwizardAppRule<SdaPlatformConfiguration> DW = dropwizardTestAppFrom(StarterApp.class)
         .withConfigFrom(SdaPlatformConfiguration::new)
         .withRandomPorts()
         .withRootPath("/api/*")
         .build();

   @Test
   public void traceTokenFilterHasHighestPriority() {
      // Make sure that trace token filter is even executed when authentication fails
      Response response = DW
            .client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("api")
            .path("ping")
            .request(APPLICATION_JSON)
            .header("Trace-Token", "MyTraceToken")
            .get();

      assertThat(response.getHeaderString("Trace-Token")).isEqualTo("MyTraceToken");
   }

   @Test
   public void consumerTokenValidationHappensBeforeAuthentication() {
      // Make sure that the consumer token filter fails before the authentication filter
      Response response = DW
          .client()
          .target("http://localhost:" + DW.getLocalPort())
          .path("api")
          .path("ping")
          .request(APPLICATION_JSON)
          .get();

      assertThat(response.readEntity(String.class)).contains("Consumer token is required");
   }
}
