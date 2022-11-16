package org.sdase.commons.server.opa.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ConfigOverride.config;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

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
import org.sdase.commons.server.opa.testing.test.PrincipalInfo;

class OpaResponsesIT {

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
          config("opa.policyPackage", "my.policy"));

  @BeforeEach
  void before() {
    WIRE.resetAll();
  }

  private void mock(int status, String body) {
    WIRE.stubFor(
        post("/v1/data/my/policy")
            .withRequestBody(
                equalToJson(
                    "{\n"
                        + "  \"input\": {\n"
                        + "    \"trace\": null,\n"
                        + "    \"jwt\":null,\n"
                        + "    \"path\": [\"resources\"],\n"
                        + "    \"httpMethod\":\"GET\"\n"
                        + "  }\n" // NOSONAR
                        + "}",
                    true,
                    true))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(status)
                    .withBody(body)));
  }

  @Test
  @RetryingTest(5)
  void shouldAllowAccess() {
    // given
    mock(
        200,
        "{\n"
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
  @RetryingTest(5)
  void shouldAllowAccessWithConstraints() {
    // given
    mock(
        200,
        "{\n"
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
        .contains(entry("customer_ids", asList("1", "2")), entry("agent_ids", singletonList("A1")));
    assertThat(principalInfo.getConstraints().isFullAccess()).isTrue();
    assertThat(principalInfo.getJwt()).isNull();
  }

  @Test
  @RetryingTest(5)
  void shouldDenyAccess() {
    // given
    mock(200, "{\n" + "  \"result\": {\n" + "    \"allow\": false\n" + "    }\n" + "  }\n" + "}");

    // when
    Response response = doGetRequest();

    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_FORBIDDEN);
  }

  @Test
  @RetryingTest(5)
  void shouldDenyAccessWithNonMatchingConstraintResponse() {
    // given
    mock(
        200,
        "{\n"
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
  @RetryingTest(5)
  void shouldAllowAccessWithNonMatchingConstraintResponse() {
    // given
    mock(
        200,
        "{\n"
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
  @RetryingTest(5)
  void shouldDenyAccessIfOpaResponseIsBroken() {
    // given
    mock(500, "");

    // when
    Response response = doGetRequest();

    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_FORBIDDEN);
  }

  @Test
  @RetryingTest(5)
  void shouldDenyAccessIfOpaResponseEmpty() {
    // given
    mock(200, "");

    // when
    Response response = doGetRequest();

    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_FORBIDDEN);
  }

  private Response doGetRequest() {
    return DW.client()
        .target("http://localhost:" + DW.getLocalPort())
        .path("resources")
        .request()
        .get();
  }
}
