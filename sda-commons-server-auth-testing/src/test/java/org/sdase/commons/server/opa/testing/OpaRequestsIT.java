package org.sdase.commons.server.opa.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.io.IOException;
import java.util.Map;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.RetryingTest;
import org.sdase.commons.client.jersey.wiremock.testing.WireMockClassExtension;
import org.sdase.commons.server.auth.testing.AuthClassExtension;
import org.sdase.commons.server.opa.testing.test.OpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.OpaBundleTestApp;

class OpaRequestsIT {

  @RegisterExtension
  @Order(0)
  private static final AuthClassExtension AUTH = AuthClassExtension.builder().build();

  @RegisterExtension
  @Order(1)
  private static final WireMockClassExtension WIRE =
      new WireMockClassExtension(wireMockConfig().dynamicPort());

  @RegisterExtension
  @Order(2)
  private static final DropwizardAppExtension<OpaBundeTestAppConfiguration> DW =
      new DropwizardAppExtension<>(
          OpaBundleTestApp.class,
          ResourceHelpers.resourceFilePath("test-config.yaml"),
          config("opa.baseUrl", WIRE::baseUrl),
          config("opa.policyPackage", "my.policy"));

  private void mock() {
    WIRE.stubFor(
        post("/v1/data/my/policy")
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(
                        "{\n"
                            + "  \"result\": {\n" // NOSONAR
                            + "    \"allow\": true\n"
                            + "  }\n"
                            + "}")));
  }

  @BeforeEach
  void before() {
    WIRE.resetAll();
    mock();
  }

  @Test
  @RetryingTest(5)
  void shouldSerializePathAndMethodCorrectly() throws IOException {
    // when
    doGetRequest(null);

    // then
    String body = WIRE.getAllServeEvents().get(0).getRequest().getBodyAsString();
    Map<String, Object> opaInput = new ObjectMapper().readValue(body, new TypeReference<>() {});

    basicAssertions(opaInput);
    assertThat(opaInput).extracting("input").extracting("trace").isNull(); // NOSONAR
    assertThat(opaInput).extracting("input").extracting("jwt").isNull();
  }

  @Test
  @RetryingTest(5)
  void shouldSerializeJwtCorrectly() throws IOException {
    // when
    MultivaluedMap<String, Object> headers = AUTH.auth().buildAuthHeader();
    doGetRequest(headers);

    // then
    String body = WIRE.getAllServeEvents().get(0).getRequest().getBodyAsString();
    Map<String, Object> opaInput = new ObjectMapper().readValue(body, new TypeReference<>() {});

    basicAssertions(opaInput);
    assertThat(opaInput).extracting("input").extracting("trace").isNull();
    assertThat(opaInput)
        .extracting("input")
        .extracting("jwt")
        .isEqualTo(((String) headers.getFirst("Authorization")).substring("Bearer ".length()));
  }

  @Test
  @RetryingTest(5)
  void shouldSerializeTraceTokenCorrectly() throws IOException {
    // when
    MultivaluedMap<String, Object> headers = AUTH.auth().buildAuthHeader();
    headers.add("Trace-Token", "myTrace");
    doGetRequest(headers);

    // then
    String body = WIRE.getAllServeEvents().get(0).getRequest().getBodyAsString();
    Map<String, Object> opaInput = new ObjectMapper().readValue(body, new TypeReference<>() {});

    basicAssertions(opaInput);
    assertThat(opaInput).extracting("input").extracting("trace").isEqualTo("myTrace");
  }

  @Test
  @RetryingTest(5)
  void shouldSerializeAdditionalHeaderCorrectly() throws IOException {
    // when
    MultivaluedMap<String, Object> headers = AUTH.auth().buildAuthHeader();
    headers.add("Simple", "SimpleValue");
    headers.add("Complex", "1");
    headers.add("Complex", "2");
    doGetRequest(headers);

    // then
    String body = WIRE.getAllServeEvents().get(0).getRequest().getBodyAsString();
    Map<String, Object> opaInput = new ObjectMapper().readValue(body, new TypeReference<>() {});

    basicAssertions(opaInput);
    //noinspection unchecked,ConstantConditions
    Map<String, Object> extractedHeaders =
        (Map<String, Object>) ((Map<String, Object>) opaInput.get("input")).get("headers");
    assertThat(extractedHeaders).extracting("simple").asList().containsExactly("SimpleValue");
    assertThat(extractedHeaders).extracting("complex").asList().containsExactly("1,2");
  }

  private void doGetRequest(MultivaluedMap<String, Object> headers) {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("resources")
            .request()
            .headers(headers)
            .get();

    assertThat(response.getStatus()).isEqualTo(200);
  }

  private void basicAssertions(Map<String, Object> opaInput) {
    assertThat(opaInput).extracting("input").extracting("httpMethod").isEqualTo("GET");
    assertThat(opaInput)
        .extracting("input")
        .extracting("path")
        .asList()
        .containsExactly("resources");
  }
}
