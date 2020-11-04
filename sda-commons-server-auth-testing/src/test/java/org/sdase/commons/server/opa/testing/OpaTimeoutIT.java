package org.sdase.commons.server.opa.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.opa.testing.test.OpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.OpaBundleTestApp;
import org.sdase.commons.server.testing.Retry;
import org.sdase.commons.server.testing.RetryRule;

public class OpaTimeoutIT {

  private static final WireMockClassRule WIRE =
      new WireMockClassRule(wireMockConfig().dynamicPort());

  private static final DropwizardAppRule<OpaBundeTestAppConfiguration> DW =
      new DropwizardAppRule<>(
          OpaBundleTestApp.class,
          resourceFilePath("test-opa-config.yaml"),
          config("opa.baseUrl", WIRE::baseUrl),
          config("opa.policyPackage", "my.policy"),
          config("opa.opaClient.timeout", "100ms"));

  @ClassRule public static final RuleChain chain = RuleChain.outerRule(WIRE).around(DW);
  @Rule public final RetryRule retryRule = new RetryRule();

  @Before
  public void before() {
    WIRE.resetAll();
  }

  @Test
  @Retry(5)
  public void shouldDenyAccess() {
    WIRE.stubFor(
        post("/v1/data/my/policy")
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody("{\n" + "  \"result\": {\n" + "    \"allow\": true\n" + "  }\n" + "}")
                    .withFixedDelay(400)));

    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("resources")
            .request()
            .get();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_FORBIDDEN);
  }

  @Test
  @Retry(5)
  public void shouldGrantAccess() {
    WIRE.stubFor(
        post("/v1/data/my/policy")
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody("{\n" + "  \"result\": {\n" + "    \"allow\": true\n" + "  }\n" + "}")
                    .withFixedDelay(1)));

    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("resources")
            .request()
            .get();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
  }
}
