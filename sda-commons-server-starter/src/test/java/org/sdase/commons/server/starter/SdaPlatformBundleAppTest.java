package org.sdase.commons.server.starter;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.auth.config.AuthConfig;
import org.sdase.commons.server.cors.CorsConfiguration;
import org.sdase.commons.server.starter.test.StarterApp;

import javax.ws.rs.core.GenericType;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sdase.commons.server.testing.DropwizardRuleHelper.dropwizardTestAppFrom;

public class SdaPlatformBundleAppTest {

   @ClassRule
   public static final DropwizardAppRule<SdaPlatformConfiguration> DW = dropwizardTestAppFrom(StarterApp.class)
         .withConfigFrom(SdaPlatformConfiguration::new)
         .withRandomPorts()
         .withConfigurationModifier(c -> c.setAuth(new AuthConfig().setDisableAuth(true)))
         .withConfigurationModifier(c -> c.setCors(new CorsConfiguration()))
         .withRootPath("/api/*")
         .build();

   @Test
   public void pongForPing() {
      Map<String, String> actual = DW.client().target("http://localhost:" + DW.getLocalPort())
            .path("api").path("ping")
            .request(APPLICATION_JSON)
            .header("Consumer-token", "test-consumer")
            .get(new GenericType<Map<String, String>>() {});

      assertThat(actual).containsExactly(entry("ping", "pong"));
   }
}
