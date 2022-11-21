package org.sdase.commons.server.kafka.health;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.SortedSet;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.kafka.KafkaBundle;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import org.sdase.commons.server.kafka.dropwizard.KafkaWithoutHealthCheckTestApplication;

class KafkaWithoutHealthCheckIT {

  @RegisterExtension
  @Order(0)
  private static final SharedKafkaTestResource KAFKA =
      new SharedKafkaTestResource()
          // we only need one consumer offsets partition
          .withBrokerProperty("offsets.topic.num.partitions", "1")
          // we don't need to wait that a consumer group rebalances since we always start with a
          // fresh kafka instance
          .withBrokerProperty("group.initial.rebalance.delay.ms", "0");

  @RegisterExtension
  @Order(1)
  private static final DropwizardAppExtension<KafkaTestConfiguration> DW =
      new DropwizardAppExtension<>(
          KafkaWithoutHealthCheckTestApplication.class,
          resourceFilePath("test-config-default.yml"),
          config("kafka.brokers", KAFKA::getKafkaConnectString));

  private KafkaWithoutHealthCheckTestApplication app;
  private static WebTarget adminTarget;

  @BeforeEach
  void before() {
    app = DW.getApplication();
    adminTarget = DW.client().target(String.format("http://localhost:%d/", DW.getAdminPort()));
  }

  @Test
  void healthCheckShouldNotContainKafka() {
    SortedSet<String> checks = app.healthCheckRegistry().getNames();
    assertThat(checks).isNotEmpty().doesNotContain(KafkaBundle.HEALTHCHECK_NAME);
  }

  @Test
  void externalHealthCheckShouldContainKafka() {
    Response response = adminTarget.path("healthcheck").request().get();
    String healthChecks = response.readEntity(String.class);
    assertThat(healthChecks).contains(KafkaBundle.EXTERNAL_HEALTHCHECK_NAME);
  }
}
