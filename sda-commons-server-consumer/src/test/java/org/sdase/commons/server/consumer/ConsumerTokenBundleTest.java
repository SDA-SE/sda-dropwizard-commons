package org.sdase.commons.server.consumer;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.consumer.test.ConsumerTokenRequiredTestApp;
import org.sdase.commons.server.consumer.test.ConsumerTokenTestApp;
import org.sdase.commons.server.consumer.test.ConsumerTokenTestConfig;
import org.sdase.commons.shared.api.error.ApiError;

class ConsumerTokenBundleTest {

  @RegisterExtension
  @Order(0)
  static final DropwizardAppExtension<ConsumerTokenTestConfig> DW =
      new DropwizardAppExtension<>(
          ConsumerTokenTestApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<ConsumerTokenTestConfig> DW_REQUIRED =
      new DropwizardAppExtension<>(
          ConsumerTokenRequiredTestApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

  @Test
  void shouldReadConsumerToken() {
    String consumerToken =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/api/token")
            .request(APPLICATION_JSON)
            .header("Consumer-Token", "test-consumer")
            .get(String.class);
    assertThat(consumerToken).isEqualTo("test-consumer");
  }

  @Test
  void shouldReadConsumerName() {
    String consumerToken =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/api/name")
            .request(APPLICATION_JSON)
            .header("Consumer-Token", "test-consumer")
            .get(String.class);
    assertThat(consumerToken).isEqualTo("test-consumer");
  }

  @Test
  void shouldRejectRequestWithoutConsumerToken() {
    try (Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/api/name")
            .request(APPLICATION_JSON)
            .get()) {
      assertThat(response.getStatus()).isEqualTo(401);
      assertThat(response.readEntity(ApiError.class).getTitle())
          .isEqualTo("Consumer token is required to access this resource.");
    }
  }

  @Test
  void shouldNotRejectRequestWithoutConsumerTokenExcludedSwagger() {
    try (Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/api/swagger.json")
            .request(APPLICATION_JSON)
            .get()) {
      assertThat(response.getStatus()).isEqualTo(200);
    }
  }

  @Test
  void shouldNotRejectRequestWithoutConsumerTokenExcludedOpenApi() {
    try (Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/api/openapi.json")
            .request(APPLICATION_JSON)
            .get()) {
      assertThat(response.getStatus()).isEqualTo(200);
    }
  }

  @Test
  void shouldNotRejectOptionsRequest() {
    try (Response response =
        DW.client().target("http://localhost:" + DW.getLocalPort()).request().options()) {
      assertThat(response.getStatus()).isEqualTo(200);
    }
  }

  @Test
  void shouldReadConsumerTokenFixedConfig() {
    String consumerToken =
        DW_REQUIRED
            .client()
            .target("http://localhost:" + DW_REQUIRED.getLocalPort())
            .path("/api/token")
            .request(APPLICATION_JSON)
            .header("Consumer-Token", "test-consumer")
            .get(String.class);
    assertThat(consumerToken).isEqualTo("test-consumer");
  }

  @Test
  void shouldReadConsumerNameFixedConfig() {
    String consumerToken =
        DW_REQUIRED
            .client()
            .target("http://localhost:" + DW_REQUIRED.getLocalPort())
            .path("/api/name")
            .request(APPLICATION_JSON)
            .header("Consumer-Token", "test-consumer")
            .get(String.class);
    assertThat(consumerToken).isEqualTo("test-consumer");
  }

  @Test
  void shouldRejectRequestWithoutConsumerTokenFixedConfig() {
    try (Response response =
        DW_REQUIRED
            .client()
            .target("http://localhost:" + DW_REQUIRED.getLocalPort())
            .path("/api/name")
            .request(APPLICATION_JSON)
            .get()) {
      assertThat(response.getStatus()).isEqualTo(401);
      assertThat(response.readEntity(ApiError.class).getTitle())
          .isEqualTo("Consumer token is required to access this resource.");
    }
  }

  @Test
  void shouldNotRejectRequestWithoutConsumerTokenExcludedFixedConfigSwagger() {
    try (Response response =
        DW_REQUIRED
            .client()
            .target("http://localhost:" + DW_REQUIRED.getLocalPort())
            .path("/api/swagger.json")
            .request(APPLICATION_JSON)
            .get()) {
      assertThat(response.getStatus()).isEqualTo(200);
    }
  }

  @Test
  void shouldNotRejectRequestWithoutConsumerTokenExcludedFixedConfigOpenApi() {
    try (Response response =
        DW_REQUIRED
            .client()
            .target("http://localhost:" + DW_REQUIRED.getLocalPort())
            .path("/api/openapi.json")
            .request(APPLICATION_JSON)
            .get()) {
      assertThat(response.getStatus()).isEqualTo(200);
    }
  }

  @Test
  void shouldNotRejectOptionsRequestFixedConfig() {
    try (Response response =
        DW_REQUIRED
            .client()
            .target("http://localhost:" + DW_REQUIRED.getLocalPort())
            .request()
            .options()) {
      assertThat(response.getStatus()).isEqualTo(200);
    }
  }
}
