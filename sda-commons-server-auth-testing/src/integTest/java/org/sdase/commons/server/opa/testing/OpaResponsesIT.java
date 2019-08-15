package org.sdase.commons.server.opa.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.collect.Lists;
import io.dropwizard.testing.junit.DropwizardAppRule;

import javax.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.opa.testing.test.OpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.OpaBundleTestApp;
import org.sdase.commons.server.opa.testing.test.PrincipalInfo;
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.sdase.commons.server.testing.LazyRule;

public class OpaResponsesIT {

   private static final WireMockClassRule WIRE = new WireMockClassRule(wireMockConfig().dynamicPort());

   private static final LazyRule<DropwizardAppRule<OpaBundeTestAppConfiguration>> DW = new LazyRule<>(
         () -> DropwizardRuleHelper
               .dropwizardTestAppFrom(OpaBundleTestApp.class)
               .withConfigFrom(OpaBundeTestAppConfiguration::new)
               .withRandomPorts()
               .withConfigurationModifier(c -> c.getOpa().setBaseUrl(WIRE.baseUrl()).setPolicyPackage("my.policy")) // NOSONAR
               .build());

   @ClassRule
   public static final RuleChain chain = RuleChain.outerRule(WIRE).around(DW);

   @BeforeClass
   public static void start() {
      WIRE.start();
   }

   @Before
   public void before() {
      WIRE.resetAll();
   }

   private void mock(int status, String body) {
      WIRE.stubFor(post("/v1/data/my/policy")
          .withRequestBody(equalToJson("{\n"
              + "  \"input\": {\n"
              + "    \"trace\": null,\n"
              + "    \"jwt\":null,\n"
              + "    \"path\": [\"resources\"],\n"
              + "    \"httpMethod\":\"GET\"\n"
              + "  }\n" // NOSONAR
              + "}"))
          .willReturn(aResponse()
              .withHeader("Content-Type", "application/json")
              .withStatus(status)
              .withBody(body)
          )
      );
   }

   @Test
   public void shouldAllowAccess() {
      // given
      mock(200, "{\n"
          + "  \"result\": {\n" // NOSONAR
          + "    \"allow\": true\n"
          + "  }\n"
          + "}");

      // when
      Response response = doGetRequest();

      // then
      assertThat(response.getStatus()).isEqualTo(SC_OK);
      PrincipalInfo principalInfo = response.readEntity(PrincipalInfo.class);
      assertThat(principalInfo.getConstraints().getConstraint()).isNull();
      assertThat(principalInfo.getConstraints().isFullAccess()).isFalse();
      assertThat(principalInfo.getJwt()).isNull();
   }

   @Test
   public void shouldAllowAccessWithConstraints() {
      // given
      mock(200, "{\n"
                      + "  \"result\": {\n"
                      + "    \"allow\": true,\n"
                      + "    \"fullAccess\": true,\n"
                      + "    \"constraint\": {\n"
                      + "      \"customer_ids\": [\"1\", \"2\"],"
                      + "      \"agent_ids\": [\"A1\"]\n"
                      + "    }\n" // NOSONAR
                      + "  }\n"
                      + "}");

      // when
      Response response = doGetRequest();

      // then
      assertThat(response.getStatus()).isEqualTo(SC_OK);
      PrincipalInfo principalInfo = response.readEntity(PrincipalInfo.class);

      assertThat(principalInfo.getConstraints().getConstraint())
            .contains(entry("customer_ids", Lists.newArrayList("1", "2")),
                  entry("agent_ids", Lists.newArrayList("A1")));
      assertThat(principalInfo.getConstraints().isFullAccess()).isTrue();
      assertThat(principalInfo.getJwt()).isNull();
   }

   @Test
   public void shouldDenyAccess() {
      // given
      mock(200, "{\n"
                  + "  \"result\": {\n"
                  + "    \"allow\": false\n"
                  + "    }\n"
                  + "  }\n"
                  + "}");

      // when
      Response response = doGetRequest();

      // then
      assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_FORBIDDEN);
   }

   @Test
   public void shouldDenyAccessWithNonMatchingConstraintResponse() {
      // given
      mock(200, "{\n"
                  + "  \"result\": {\n"
                  + "    \"allow\": false,\n"
                  + "    \"abc\": {\n"
                  + "    }\n"
                  + "  }\n"
                  + "}");

      // when
      Response response = doGetRequest();

      // then
      assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_FORBIDDEN);
   }

   @Test
   public void shouldAllowAccessWithNonMatchingConstraintResponse() {
      // given
      mock(200, "{\n"
          + "  \"result\": {\n"
          + "    \"allow\": true,\n"
          + "    \"abc\": {\n"
          + "    }\n"
          + "  }\n"
          + "}");

      // when
      Response response = doGetRequest();

      // then
      assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
      PrincipalInfo principalInfo = response.readEntity(PrincipalInfo.class);
      assertThat(principalInfo.getConstraints().getConstraint()).isNull();
      assertThat(principalInfo.getConstraints().isFullAccess()).isFalse();
      assertThat(principalInfo.getJwt()).isNull();
   }

   @Test
   public void shouldDenyAccessIfOpaResponseIsBroken() {
      // given
      mock(500, "");

      // when
      Response response = doGetRequest();

      // then
      assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_FORBIDDEN);
   }

   @Test
   public void shouldDenyAccessIfOpaResponseEmpty() {
      // given
      // given
      mock(200, "");

      // when
      Response response = doGetRequest();

      // then
      assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_FORBIDDEN);
   }


   private Response doGetRequest() {
      return DW.getRule().client().target("http://localhost:" + DW.getRule().getLocalPort()).path("resources").request().get();
   }
}
