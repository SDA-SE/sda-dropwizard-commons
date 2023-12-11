package org.sdase.commons.server.auth.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.dropwizard.testing.ConfigOverride.config;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.HttpHeaders.USER_AGENT;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.sdase.commons.server.auth.testing.test.AuthTestApp;
import org.sdase.commons.server.auth.testing.test.AuthTestConfig;

/** A test that checks if the jersey client that is used to load keys is configurable */
@SetSystemProperty(key = AuthClassExtension.AUTH_ENV_KEY, value = "{\"keys\": [{}]}")
class CustomKeyLoaderConfigIT {

  @RegisterExtension
  @Order(0)
  static final WireMockExtension WIRE = new WireMockExtension.Builder().build();

  @RegisterExtension
  @Order(2)
  static final DropwizardAppExtension<AuthTestConfig> DW =
      new DropwizardAppExtension<>(
          AuthTestApp.class,
          ResourceHelpers.resourceFilePath("test-config.yaml"),
          // add a custom keyLoader config
          config("auth.keyLoaderClient.userAgent", "my-user-agent"),
          config("auth.keys[0].type", "JWKS"),
          config("auth.keys[0].location", () -> WIRE.url("jwks")));

  @Test
  void shouldSendCustomUserAgentInTheJwksRequest() {
    WIRE.stubFor(get(anyUrl()).willReturn(okJson("{\"keys\": []}")));

    final String token =
        "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.e30.sJ38ARdiqW5NDXRzkwPGD_XVVBL_q50ytQr3CezUaWCUlDgOwa49G_GuiriVbAAhllyETulropgTvCxbsDdXOHW4YrQWrJ1rn-HLqceoNxSX_Z2HaR5CeNtUmGL2pX-kv_9rYmyjRVwcOMRsQx_a7DPl-Bo5RrKXHka1nnaQ1a55W4PPOSiCCq4oEYH6RerxODh7uvfB9cYruUMH60f-kZeMVVzKuFpwBdI8xCYEZxXcBPtERsOVBTnGpr8S2_2xpaP6vfLsY4M63GwRNsTL9e8Ghm5n7VMuMrJESCHSrCTMMAK90S_iA3VwbVSUMyrJNdeccAc4lBqizUb7JuBygA";
    try (Response response =
        createWebTarget()
            .path("/secure") // NOSONAR
            .request(APPLICATION_JSON)
            .header(AUTHORIZATION, "Bearer " + token)
            .get()) {

      assertThat(response.getStatus()).isEqualTo(SC_UNAUTHORIZED);
    }

    WIRE.verify(
        getRequestedFor(urlEqualTo("/jwks")).withHeader(USER_AGENT, equalTo("my-user-agent")));
  }

  private WebTarget createWebTarget() {
    return DW.client().target("http://localhost:" + DW.getLocalPort());
  }
}
