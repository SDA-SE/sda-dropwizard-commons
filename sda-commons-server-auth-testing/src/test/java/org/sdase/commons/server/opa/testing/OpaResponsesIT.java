package org.sdase.commons.server.opa.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static io.dropwizard.testing.ConfigOverride.config;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.core.Response;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.RetryingTest;
import org.sdase.commons.server.opa.testing.test.OpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.OpaBundleTestApp;
import org.sdase.commons.server.opa.testing.test.PrincipalInfo;

class OpaResponsesIT {

  @RegisterExtension
  @Order(0)
  static final WireMockExtension WIRE = new WireMockExtension.Builder().build();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<OpaBundeTestAppConfiguration> DW =
      new DropwizardAppExtension<>(
          OpaBundleTestApp.class,
          ResourceHelpers.resourceFilePath("test-opa-config.yaml"),
          config("opa.baseUrl", WIRE::baseUrl),
          config("opa.policyPackage", "my.policy"));

  @BeforeEach
  void beforeEach() {
    WIRE.resetAll();
  }

  private void mock(int status, String body) {
    // NOSONAR
    WIRE.stubFor(
        post("/v1/data/my/policy")
            .withRequestBody(
                equalToJson(
                    """
                                {
                                  "input": {
                                    "jwt":null,
                                    "path": ["resources"],
                                    "httpMethod":"GET"
                                  }
                                }
                              """,
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
    // NOSONAR
    mock(
        200,
        """
                      {
                        "result": {
                          "allow": true
                        }
                      }
                    """);

    // when
    try (Response response = doGetRequest()) {

      // then
      assertThat(response.getStatus()).isEqualTo(SC_OK);
      PrincipalInfo principalInfo = response.readEntity(PrincipalInfo.class);
      assertThat(principalInfo.getConstraints().getConstraint()).isNull();
      assertThat(principalInfo.getConstraints().isFullAccess()).isFalse();
      assertThat(principalInfo.getJwt()).isNull();
    }
  }

  @Test
  @RetryingTest(5)
  void shouldAllowAccessWithConstraints() {
    // given
    // NOSONAR
    mock(
        200,
        """
                      {
                        "result": {
                          "allow": true,
                          "fullAccess": true,
                          "constraint": {
                            "customer_ids": ["1", "2"],
                            "agent_ids": ["A1"]
                          }
                        }
                      }
                    """);

    // when
    try (Response response = doGetRequest()) {

      // then
      assertThat(response.getStatus()).isEqualTo(SC_OK);
      PrincipalInfo principalInfo = response.readEntity(PrincipalInfo.class);

      assertThat(principalInfo.getConstraints().getConstraint())
          .contains(
              entry("customer_ids", asList("1", "2")), entry("agent_ids", singletonList("A1")));
      assertThat(principalInfo.getConstraints().isFullAccess()).isTrue();
      assertThat(principalInfo.getJwt()).isNull();
    }
  }

  @Test
  @RetryingTest(5)
  void shouldDenyAccess() {
    // given
    mock(
        200,
        """
                {
                  "result": {
                    "allow": false
                    }
                  }
                }
              """);

    // when
    try (Response response = doGetRequest()) {

      // then
      assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_FORBIDDEN);
    }
  }

  @Test
  @RetryingTest(5)
  void shouldDenyAccessWithNonMatchingConstraintResponse() {
    // given
    mock(
        200,
        """
                {
                  "result": {
                    "allow": false,
                    "abc": {
                    }
                  }
                }
              """);

    // when
    try (Response response = doGetRequest()) {

      // then
      assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_FORBIDDEN);
    }
  }

  @Test
  @RetryingTest(5)
  void shouldAllowAccessWithNonMatchingConstraintResponse() {
    // given
    mock(
        200,
        """
                {
                  "result": {
                    "allow": true,
                    "abc": {
                    }
                  }
                }
              """);

    // when
    try (Response response = doGetRequest()) {

      // then
      assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
      PrincipalInfo principalInfo = response.readEntity(PrincipalInfo.class);
      assertThat(principalInfo.getConstraints().getConstraint()).isNull();
      assertThat(principalInfo.getConstraints().isFullAccess()).isFalse();
      assertThat(principalInfo.getJwt()).isNull();
    }
  }

  @Test
  @RetryingTest(5)
  void shouldDenyAccessIfOpaResponseIsBroken() {
    // given
    mock(500, "");

    // when
    try (Response response = doGetRequest()) {

      // then
      assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_FORBIDDEN);
    }
  }

  @Test
  @RetryingTest(5)
  void shouldDenyAccessIfOpaResponseEmpty() {
    // given
    mock(200, "");

    // when
    try (Response response = doGetRequest()) {

      // then
      assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_FORBIDDEN);
    }
  }

  private Response doGetRequest() {
    return DW.client()
        .target("http://localhost:" + DW.getLocalPort())
        .path("resources")
        .request()
        .get();
  }
}
