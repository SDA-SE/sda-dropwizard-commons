package org.sdase.commons.server.consumer;

import org.junit.rules.RuleChain;
import org.sdase.commons.server.consumer.test.ConsumerTokenOptionalTestApp;
import org.sdase.commons.server.consumer.test.ConsumerTokenRequiredTestApp;
import org.sdase.commons.server.consumer.test.ConsumerTokenTestApp;
import org.sdase.commons.server.consumer.test.ConsumerTokenTestConfig;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.testing.EnvironmentRule;
import org.sdase.commons.shared.api.error.ApiError;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

public class ConsumerTokenBundleTest {


   @ClassRule
   public static DropwizardAppRule<ConsumerTokenTestConfig> DW = new DropwizardAppRule<>(
         ConsumerTokenTestApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

   @ClassRule
   public static DropwizardAppRule<ConsumerTokenTestConfig> DW_REQUIRED = new DropwizardAppRule<>(
         ConsumerTokenRequiredTestApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

   @Test
   public void shouldReadConsumerToken() {
      String consumerToken = DW.client().target("http://localhost:" + DW.getLocalPort()).path("/api/token")
            .request(APPLICATION_JSON)
            .header("Consumer-Token", "test-consumer")
            .get(String.class);
      assertThat(consumerToken).isEqualTo("test-consumer");
   }

   @Test
   public void shouldReadConsumerName() {
      String consumerToken = DW.client().target("http://localhost:" + DW.getLocalPort()).path("/api/name")
            .request(APPLICATION_JSON)
            .header("Consumer-Token", "test-consumer")
            .get(String.class);
      assertThat(consumerToken).isEqualTo("test-consumer");
   }

   @Test
   public void shouldRejectRequestWithoutConsumerToken() {
      Response response = DW.client().target("http://localhost:" + DW.getLocalPort()).path("/api/name")
            .request(APPLICATION_JSON).get();
      assertThat(response.getStatus()).isEqualTo(422);
      assertThat(response.readEntity(ApiError.class).getTitle()).isEqualTo("Consumer token is required to access this resource.");
   }

   @Test
   public void shouldNotRejectRequestWithoutConsumerTokenExcluded() {
      Response response = DW.client().target("http://localhost:" + DW.getLocalPort()).path("/api/swagger.json")
            .request(APPLICATION_JSON).get();
      assertThat(response.getStatus()).isEqualTo(200);
   }

   @Test
   public void shouldNotRejectOptionsRequest() {
      Response response = DW.client().target("http://localhost:" + DW.getLocalPort()).request().options();
      assertThat(response.getStatus()).isEqualTo(200);
   }

   @Test
   public void shouldReadConsumerTokenFixedConfig() {
      String consumerToken = DW_REQUIRED.client().target("http://localhost:" + DW_REQUIRED.getLocalPort())
            .path("/api/token")
            .request(APPLICATION_JSON)
            .header("Consumer-Token", "test-consumer")
            .get(String.class);
      assertThat(consumerToken).isEqualTo("test-consumer");
   }

   @Test
   public void shouldReadConsumerNameFixedConfig() {
      String consumerToken = DW_REQUIRED.client().target("http://localhost:" + DW_REQUIRED.getLocalPort()).path("/api/name")
            .request(APPLICATION_JSON)
            .header("Consumer-Token", "test-consumer")
            .get(String.class);
      assertThat(consumerToken).isEqualTo("test-consumer");
   }

   @Test
   public void shouldRejectRequestWithoutConsumerTokenFixedConfig() {
      Response response = DW_REQUIRED.client().target("http://localhost:" + DW_REQUIRED.getLocalPort()).path("/api/name")
            .request(APPLICATION_JSON).get();
      assertThat(response.getStatus()).isEqualTo(422);
      assertThat(response.readEntity(ApiError.class).getTitle()).isEqualTo("Consumer token is required to access this resource.");
   }


   @Test
   public void shouldNotRejectRequestWithoutConsumerTokenExcludedFixedConfig() {
      Response response = DW_REQUIRED.client().target("http://localhost:" + DW_REQUIRED.getLocalPort()).path("/api/swagger.json")
            .request(APPLICATION_JSON).get();
      assertThat(response.getStatus()).isEqualTo(200);
   }

   @Test
   public void shouldNotRejectOptionsRequestFixedConfig() {
      Response response = DW_REQUIRED.client().target("http://localhost:" + DW_REQUIRED.getLocalPort()).request().options();
      assertThat(response.getStatus()).isEqualTo(200);
   }
}
