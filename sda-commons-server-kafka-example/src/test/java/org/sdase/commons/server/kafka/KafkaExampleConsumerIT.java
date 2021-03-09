package org.sdase.commons.server.kafka;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.kafka.model.Key;
import org.sdase.commons.server.kafka.model.Value;

class KafkaExampleConsumerIT {

  @RegisterExtension
  @Order(0)
  static final SharedKafkaTestResource KAFKA =
      new SharedKafkaTestResource()
          .withBrokers(2)
          // we only need one consumer offsets partition
          .withBrokerProperty("offsets.topic.num.partitions", "1")
          // we don't need to wait that a consumer group rebalances since we always start with a
          // fresh kafka instance
          .withBrokerProperty("group.initial.rebalance.delay.ms", "0");

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<KafkaExampleConfiguration> DW =
      new DropwizardAppExtension<>(
          KafkaExampleConsumerApplication.class,
          ResourceHelpers.resourceFilePath("test-config-consumer.yml"),
          config("kafka.brokers", KAFKA::getKafkaConnectString));

  private static final String TOPIC_NAME = "exampleTopic";

  @Test
  void testUseConsumer() throws JsonProcessingException {
    // given
    KafkaExampleConsumerApplication application = DW.getApplication();
    final String key = "key";
    final String v1 = "v1";
    final String v2 = "v2";

    Map<byte[], byte[]> records = new HashMap<>();
    ObjectMapper objectMapper = new ObjectMapper();
    records.put(
        objectMapper.writeValueAsBytes(new Key(key)),
        objectMapper.writeValueAsBytes(new Value(v1, v2)));
    KAFKA.getKafkaTestUtils().produceRecords(records, TOPIC_NAME, 0);

    // then
    await().atMost(10, TimeUnit.SECONDS).until(() -> !application.getReceivedMessages().isEmpty());

    assertThat(application.getReceivedMessages().get(0))
        .isEqualToComparingFieldByField(new Value(v1, v2));
  }

  @Test
  void testUseConsumerWithConfiguration() {
    // given
    KafkaExampleConsumerApplication application = DW.getApplication();

    Map<byte[], byte[]> records = new HashMap<>();
    records.put(new byte[] {0, 0, 0, 0, 0, 0, 0, 1}, new byte[] {0, 0, 0, 0, 0, 0, 0, 2});
    KAFKA.getKafkaTestUtils().produceRecords(records, "exampleTopicConfiguration", 0);

    // then
    await().atMost(10, TimeUnit.SECONDS).until(() -> !application.getReceivedLongs().isEmpty());

    assertThat(application.getReceivedLongs()).contains(2L);
  }
}
