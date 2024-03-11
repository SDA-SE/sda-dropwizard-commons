package org.sdase.commons.server.opa.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.auth.testing.AuthClassExtension;
import org.sdase.commons.server.opa.testing.test.OpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.OpaBundleTestApp;

class OpaRequestsIT {

  @RegisterExtension
  @Order(0)
  static final AuthClassExtension AUTH = AuthClassExtension.builder().build();

  @RegisterExtension
  @Order(1)
  static final WireMockExtension WIRE = new WireMockExtension.Builder().build();

  @RegisterExtension
  @Order(2)
  static final DropwizardAppExtension<OpaBundeTestAppConfiguration> DW =
      new DropwizardAppExtension<>(
          OpaBundleTestApp.class,
          ResourceHelpers.resourceFilePath("test-config.yaml"),
          config("opa.baseUrl", WIRE::baseUrl),
          config("opa.policyPackage", "my.policy"));

  private void mock() {
    // NOSONAR
    WIRE.stubFor(
        post("/v1/data/my/policy")
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(
                        """
                        {
                          "result": {
                            "allow": true
                          }
                        }
                        """)));
  }

  @BeforeEach
  void before() {
    WIRE.resetAll();
    mock();
  }

  @Test
  void shouldSerializePathAndMethodCorrectly() throws IOException {
    // when
    doGetRequest(null);

    // then
    String body = WIRE.getAllServeEvents().get(0).getRequest().getBodyAsString();
    Map<String, Object> opaInput = new ObjectMapper().readValue(body, new TypeReference<>() {});

    basicAssertions(opaInput);
    assertThat(opaInput).extracting("input").extracting("trace").asString().isNotBlank();
    assertThat(opaInput).extracting("input").extracting("jwt").isNull();
  }

  @Test
  void shouldSerializeJwtCorrectly() throws IOException {
    // when
    MultivaluedMap<String, Object> headers = AUTH.auth().buildAuthHeader();
    doGetRequest(headers);

    // then
    String body = WIRE.getAllServeEvents().get(0).getRequest().getBodyAsString();
    Map<String, Object> opaInput = new ObjectMapper().readValue(body, new TypeReference<>() {});

    basicAssertions(opaInput);
    assertThat(opaInput).extracting("input").extracting("trace").asString().isNotBlank();
    assertThat(opaInput)
        .extracting("input")
        .extracting("jwt")
        .isEqualTo(((String) headers.getFirst("Authorization")).substring("Bearer ".length()));
  }

  @Test
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
    //noinspection unchecked
    Map<String, Object> extractedHeaders =
        (Map<String, Object>) ((Map<String, Object>) opaInput.get("input")).get("headers");
    assertThat(extractedHeaders)
        .extracting("simple")
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .containsExactly("SimpleValue");
    assertThat(extractedHeaders)
        .extracting("complex")
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .containsExactly("1,2");
  }

  private void doGetRequest(MultivaluedMap<String, Object> headers) {
    try (Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("resources")
            .request()
            .headers(headers)
            .get()) {

      assertThat(response.getStatus()).isEqualTo(200);
    }
  }

  private void basicAssertions(Map<String, Object> opaInput) {
    assertThat(opaInput).extracting("input").extracting("httpMethod").isEqualTo("GET");
    assertThat(opaInput)
        .extracting("input")
        .extracting("path")
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .containsExactly("resources");
  }
}
