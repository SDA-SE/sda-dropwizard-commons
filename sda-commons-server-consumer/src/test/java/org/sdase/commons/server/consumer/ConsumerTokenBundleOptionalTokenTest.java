package org.sdase.commons.server.consumer;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.sdase.commons.server.consumer.test.ConsumerTokenOptionalTestApp;
import org.sdase.commons.server.consumer.test.ConsumerTokenTestApp;
import org.sdase.commons.server.consumer.test.ConsumerTokenTestConfig;

@SetSystemProperty(key = "CONSUMER_TOKEN_OPTIONAL", value = "true")
class ConsumerTokenBundleOptionalTokenTest {

  @RegisterExtension
  @Order(0)
  private static final DropwizardAppExtension<ConsumerTokenTestConfig> DW =
      new DropwizardAppExtension<>(
          ConsumerTokenTestApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

  @RegisterExtension
  @Order(1)
  private static final DropwizardAppExtension<ConsumerTokenTestConfig> DW_OPTIONAL =
      new DropwizardAppExtension<>(
          ConsumerTokenOptionalTestApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

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
  void shouldNotRejectRequestWithoutConsumerToken() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/api/name")
            .request(APPLICATION_JSON)
            .get();
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void shouldNotRejectOptionsRequest() {
    Response response =
        DW.client().target("http://localhost:" + DW.getLocalPort()).request().options();
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void shouldReadConsumerTokenFixedConfig() {
    String consumerToken =
        DW_OPTIONAL
            .client()
            .target("http://localhost:" + DW_OPTIONAL.getLocalPort())
            .path("/api/token")
            .request(APPLICATION_JSON)
            .header("Consumer-Token", "test-consumer")
            .get(String.class);
    assertThat(consumerToken).isEqualTo("test-consumer");
  }

  @Test
  void shouldReadConsumerNameFixedConfig() {
    String consumerToken =
        DW_OPTIONAL
            .client()
            .target("http://localhost:" + DW_OPTIONAL.getLocalPort())
            .path("/api/name")
            .request(APPLICATION_JSON)
            .header("Consumer-Token", "test-consumer")
            .get(String.class);
    assertThat(consumerToken).isEqualTo("test-consumer");
  }

  @Test
  void shouldNotRejectRequestWithoutConsumerTokenFixedConfig() {
    Response response =
        DW_OPTIONAL
            .client()
            .target("http://localhost:" + DW_OPTIONAL.getLocalPort())
            .path("/api/name")
            .request(APPLICATION_JSON)
            .get();
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void shouldNotRejectOptionsRequestFixedConfig() {
    Response response =
        DW_OPTIONAL
            .client()
            .target("http://localhost:" + DW_OPTIONAL.getLocalPort())
            .request()
            .options();
    assertThat(response.getStatus()).isEqualTo(200);
  }
}
