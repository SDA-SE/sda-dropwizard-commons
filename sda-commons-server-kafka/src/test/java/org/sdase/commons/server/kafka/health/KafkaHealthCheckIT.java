package org.sdase.commons.server.kafka.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.health.HealthCheck;
import com.salesforce.kafka.test.KafkaBroker;
import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.kafka.KafkaConfiguration;

class KafkaHealthCheckIT {

  @RegisterExtension
  public static final SharedKafkaTestResource KAFKA =
      new SharedKafkaTestResource()
          .withBrokers(1)
          // we only need one consumer offsets partition
          .withBrokerProperty("offsets.topic.num.partitions", "1")
          // we don't need to wait that a consumer group rebalances since we always start with a
          // fresh kafka instance
          .withBrokerProperty("group.initial.rebalance.delay.ms", "0");

  @Test
  void testHealthCheckIt() throws Exception {

    KafkaConfiguration config = new KafkaConfiguration();
    config.setBrokers(
        KAFKA.getKafkaBrokers().stream()
            .map(KafkaBroker::getConnectString)
            .collect(Collectors.toList()));
    config.getHealthCheck().setTimeoutInSeconds(5);

    KafkaHealthCheck check = new KafkaHealthCheck(config);
    HealthCheck.Result result = check.execute();
    assertThat(result.isHealthy()).isTrue();

    for (int i = 1; i <= KAFKA.getKafkaBrokers().size(); i++) {
      KAFKA.getKafkaBrokers().getBrokerById(i).stop();
    }

    HealthCheck.Result resultFalse = check.execute();
    assertThat(resultFalse.isHealthy()).isFalse();
  }
}
