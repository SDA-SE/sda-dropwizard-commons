package org.sdase.commons.server.consumer;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.ws.rs.core.Response;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.consumer.test.ConsumerTokenOptionalTestApp;
import org.sdase.commons.server.consumer.test.ConsumerTokenTestApp;
import org.sdase.commons.server.consumer.test.ConsumerTokenTestConfig;
import org.sdase.commons.server.testing.EnvironmentRule;

public class ConsumerTokenBundleOptionalTokenTest {

  private static DropwizardAppRule<ConsumerTokenTestConfig> DW =
      new DropwizardAppRule<>(
          ConsumerTokenTestApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

  private static DropwizardAppRule<ConsumerTokenTestConfig> DW_OPTIONAL =
      new DropwizardAppRule<>(
          ConsumerTokenOptionalTestApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

  @ClassRule
  public static RuleChain CHAIN =
      RuleChain.outerRule(new EnvironmentRule().setEnv("CONSUMER_TOKEN_OPTIONAL", "true"))
          .around(DW)
          .around(DW_OPTIONAL);

  @Test
  public void shouldReadConsumerToken() {
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
  public void shouldReadConsumerName() {
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
  public void shouldNotRejectRequestWithoutConsumerToken() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/api/name")
            .request(APPLICATION_JSON)
            .get();
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  public void shouldNotRejectOptionsRequest() {
    Response response =
        DW.client().target("http://localhost:" + DW.getLocalPort()).request().options();
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  public void shouldReadConsumerTokenFixedConfig() {
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
  public void shouldReadConsumerNameFixedConfig() {
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
  public void shouldNotRejectRequestWithoutConsumerTokenFixedConfig() {
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
  public void shouldNotRejectOptionsRequestFixedConfig() {
    Response response =
        DW_OPTIONAL
            .client()
            .target("http://localhost:" + DW_OPTIONAL.getLocalPort())
            .request()
            .options();
    assertThat(response.getStatus()).isEqualTo(200);
  }
}
