package org.sdase.commons.server.starter;

import io.dropwizard.testing.junit.DropwizardAppRule;
import io.prometheus.client.CollectorRegistry;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.sdase.commons.server.starter.test.StarterApp;

import static org.sdase.commons.server.testing.DropwizardRuleHelper.dropwizardTestAppFrom;

public class SwaggerBuilderTest {

   @Rule
   public DropwizardAppRule<SdaPlatformConfiguration> dw = dropwizardTestAppFrom(StarterApp.class)
         .withConfigFrom(SdaPlatformConfiguration::new)
         .withRandomPorts()
         .withConfigurationModifier(c -> c.getAuth().setDisableAuth(true))
         .build();

   @BeforeClass
   public static void reset() {
      CollectorRegistry.defaultRegistry.clear();
   }

   @Test
   public void returnSwaggerDetails() {

      String swaggerYaml = dw.client().target("http://localhost:" + dw.getLocalPort())
            .path("swagger.yaml")
            .request("application/yaml")
            .get(String.class);

      Assertions.assertThat(swaggerYaml)
            .contains("Starter")
            .contains("1.1.1")
            .contains("A test application")
            .contains("Sample License")
            .contains("John Doe")
            .contains("j.doe@example.com")
            .contains("summary: \"Answers the ping request with a pong.\"")
            .contains("description: \"The ping endpoint is usually")
            .contains("/ping")
      ;
   }
}
