package org.sdase.commons.server.kafka.health;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.SortedSet;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sdase.commons.server.kafka.KafkaBundle;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import org.sdase.commons.server.kafka.dropwizard.KafkaWithoutHealthCheckTestApplication;

public class KafkaWithoutHealthCheckIT {

  private static final SharedKafkaTestResource KAFKA =
      new SharedKafkaTestResource()
          // we only need one consumer offsets partition
          .withBrokerProperty("offsets.topic.num.partitions", "1")
          // we don't need to wait that a consumer group rebalances since we always start with a
          // fresh kafka instance
          .withBrokerProperty("group.initial.rebalance.delay.ms", "0");

  private static final DropwizardAppRule<KafkaTestConfiguration> DW =
      new DropwizardAppRule<>(
          KafkaWithoutHealthCheckTestApplication.class,
          resourceFilePath("test-config-default.yml"),
          config("kafka.brokers", KAFKA::getKafkaConnectString));

  @ClassRule public static final TestRule CHAIN = RuleChain.outerRule(KAFKA).around(DW);

  private KafkaWithoutHealthCheckTestApplication app;

  @Before
  public void before() {
    app = DW.getApplication();
  }

  @Test
  public void healthCheckShouldNotContainKafka() {
    SortedSet<String> checks = app.healthCheckRegistry().getNames();
    assertThat(checks).isNotEmpty().doesNotContain(KafkaBundle.HEALTHCHECK_NAME);
  }
}
