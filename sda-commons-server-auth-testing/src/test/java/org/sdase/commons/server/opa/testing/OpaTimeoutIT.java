package org.sdase.commons.server.opa.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.RetryingTest;
import org.sdase.commons.client.jersey.wiremock.testing.WireMockClassExtension;
import org.sdase.commons.server.opa.testing.test.OpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.OpaBundleTestApp;

class OpaTimeoutIT {

  @RegisterExtension
  @Order(0)
  private static final WireMockClassExtension WIRE =
      new WireMockClassExtension(wireMockConfig().dynamicPort());

  @RegisterExtension
  @Order(1)
  private static final DropwizardAppExtension<OpaBundeTestAppConfiguration> DW =
      new DropwizardAppExtension<>(
          OpaBundleTestApp.class,
          ResourceHelpers.resourceFilePath("test-opa-config.yaml"),
          config("opa.baseUrl", WIRE::baseUrl),
          config("opa.policyPackage", "my.policy"),
          config("opa.opaClient.timeout", "100ms"));

  @BeforeEach
  void before() {
    WIRE.resetAll();
  }

  @Test
  @RetryingTest(5)
  void shouldDenyAccess() {
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
  @RetryingTest(5)
  void shouldGrantAccess() {
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
