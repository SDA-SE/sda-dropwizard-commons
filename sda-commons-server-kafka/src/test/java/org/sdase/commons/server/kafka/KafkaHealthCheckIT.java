package org.sdase.commons.server.kafka;

import com.codahale.metrics.health.HealthCheck;
import com.salesforce.kafka.test.KafkaBroker;
import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.kafka.health.KafkaHealthCheck;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class KafkaHealthCheckIT {

   @ClassRule
   public static final SharedKafkaTestResource KAFKA = new SharedKafkaTestResource()
         .withBrokers(1)
         // we only need one consumer offsets partition
         .withBrokerProperty("offsets.topic.num.partitions", "1")
         // we don't need to wait that a consumer group rebalances since we always start with a fresh kafka instance
         .withBrokerProperty("group.initial.rebalance.delay.ms", "0");


   @Test
   public void testHealthCheckIt() throws Exception {

      KafkaConfiguration config = new KafkaConfiguration();
      config.setBrokers(KAFKA.getKafkaBrokers().stream().map(KafkaBroker::getConnectString).collect(Collectors.toList()));

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
