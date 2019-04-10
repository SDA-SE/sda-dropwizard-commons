package org.sdase.commons.server.opa.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.opa.testing.test.OpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.OpaBundleTestApp;
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.sdase.commons.server.testing.LazyRule;

public class OpaTimeoutIT {

   private static final WireMockClassRule WIRE = new WireMockClassRule(wireMockConfig().dynamicPort());

   private static final LazyRule<DropwizardAppRule<OpaBundeTestAppConfiguration>> DW = new LazyRule<>(
         () -> DropwizardRuleHelper
               .dropwizardTestAppFrom(OpaBundleTestApp.class)
               .withConfigFrom(OpaBundeTestAppConfiguration::new)
               .withRandomPorts()
               .withConfigurationModifier(c -> c.getOpa().setBaseUrl(WIRE.baseUrl()).setPolicyPackage("my.policy")) // NOSONAR
               .withConfigurationModifier(c -> c.getOpa().setReadTimeout(100))
               .build());

   @ClassRule
   public static final RuleChain chain = RuleChain.outerRule(WIRE).around(DW);

   @BeforeClass
   public static void start() {
      WIRE.start();
   }

   @Test
   public void shouldDenyAccess() {
      WIRE.stubFor(post("/v1/data/my/policy")
          .willReturn(aResponse()
              .withHeader("Content-Type", "application/json")
              .withStatus(200)
              .withBody("{\n"
                  + "  \"result\": {\n"
                  + "    \"allow\": true\n"
                  + "  }\n"
                  + "}")
              .withFixedDelay(400)
          )
      );

      Response response = DW
            .getRule()
            .client()
            .target("http://localhost:" + DW.getRule().getLocalPort())
            .path("resources")
            .request()
            .get();

      assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_FORBIDDEN);
   }


   @Test
   public void shouldGrantAccess() {
      WIRE.stubFor(post("/v1/data/my/policy")
          .willReturn(aResponse()
              .withHeader("Content-Type", "application/json")
              .withStatus(200)
              .withBody("{\n"
                  + "  \"result\": {\n"
                  + "    \"allow\": true\n"
                  + "  }\n"
                  + "}")
              .withFixedDelay(1)
          )
      );

      Response response = DW
          .getRule()
          .client()
          .target("http://localhost:" + DW.getRule().getLocalPort())
          .path("resources")
          .request()
          .get();

      assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
   }

}
