package org.sdase.commons.server.kafka.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.health.HealthCheck;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.kafka.KafkaConfiguration;
import org.sdase.commons.server.kafka.testing.KraftKafkaTestResource;

class ExternalKafkaHealthCheckIT {

  @RegisterExtension
  public static final KraftKafkaTestResource KAFKA =
      new KraftKafkaTestResource()
          // we only need one consumer offsets partition
          .withBrokerProperty("offsets.topic.num.partitions", "1")
          // we don't need to wait that a consumer group rebalances since we always start with a
          // fresh kafka instance
          .withBrokerProperty("group.initial.rebalance.delay.ms", "0");

  @Test
  void testHealthCheckIt() {

    KafkaConfiguration config = new KafkaConfiguration();
    config.setBrokers(Arrays.asList(KAFKA.getKafkaConnectString().split(",")));
    config.getHealthCheck().setTimeoutInSeconds(5);

    ExternalKafkaHealthCheck check = new ExternalKafkaHealthCheck(config);
    HealthCheck.Result result = check.execute();
    assertThat(result.isHealthy()).isTrue();

    KAFKA.stop();

    HealthCheck.Result resultFalse = check.execute();
    assertThat(resultFalse.isHealthy()).isFalse();
  }
}
