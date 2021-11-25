package org.sdase.commons.server.auth.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.dropwizard.testing.ConfigOverride.config;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.USER_AGENT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sdase.commons.server.auth.testing.test.AuthTestApp;
import org.sdase.commons.server.auth.testing.test.AuthTestConfig;

/** A test that checks if the jersey client that is used to load keys is configurable */
public class CustomKeyLoaderConfigIT {

  public static final WireMockRule WIRE =
      new WireMockRule(new WireMockConfiguration().dynamicPort());

  static {
    // the stub needs to be registered before the application starts.
    // A @BeforeClass method might be too late.
    WIRE.stubFor(get(anyUrl()).willReturn(okJson("{\"keys\": []}")));
  }

  public static final TestRule AUTH = AuthRule.createTestRule("{\"keys\": [{}]}");

  private static final DropwizardAppRule<AuthTestConfig> DW =
      new DropwizardAppRule<>(
          AuthTestApp.class,
          ResourceHelpers.resourceFilePath("test-config.yaml"),
          // add a custom keyLoader config
          config("auth.keyLoaderClient.userAgent", "my-user-agent"),
          config("auth.keys[0].type", "JWKS"),
          config("auth.keys[0].location", () -> WIRE.url("jwks")));

  @ClassRule public static RuleChain RULE = RuleChain.outerRule(WIRE).around(AUTH).around(DW);

  @Test
  public void shouldSendCustomUserAgentInTheJwksRequest() {
    final String token =
        "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.e30.sJ38ARdiqW5NDXRzkwPGD_XVVBL_q50ytQr3CezUaWCUlDgOwa49G_GuiriVbAAhllyETulropgTvCxbsDdXOHW4YrQWrJ1rn-HLqceoNxSX_Z2HaR5CeNtUmGL2pX-kv_9rYmyjRVwcOMRsQx_a7DPl-Bo5RrKXHka1nnaQ1a55W4PPOSiCCq4oEYH6RerxODh7uvfB9cYruUMH60f-kZeMVVzKuFpwBdI8xCYEZxXcBPtERsOVBTnGpr8S2_2xpaP6vfLsY4M63GwRNsTL9e8Ghm5n7VMuMrJESCHSrCTMMAK90S_iA3VwbVSUMyrJNdeccAc4lBqizUb7JuBygA";
    Response response =
        createWebTarget()
            .path("/secure") // NOSONAR
            .request(APPLICATION_JSON)
            .header(AUTHORIZATION, "Bearer " + token)
            .get();

    assertThat(response.getStatus()).isEqualTo(SC_UNAUTHORIZED);

    WIRE.verify(
        getRequestedFor(urlEqualTo("/jwks")).withHeader(USER_AGENT, equalTo("my-user-agent")));
  }

  private WebTarget createWebTarget() {
    return DW.client().target("http://localhost:" + DW.getLocalPort());
  }
}
