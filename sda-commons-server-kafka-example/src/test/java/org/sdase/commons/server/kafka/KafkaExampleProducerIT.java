package org.sdase.commons.server.kafka;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.kafka.model.Key;
import org.sdase.commons.server.kafka.model.Value;

class KafkaExampleProducerIT {

  @RegisterExtension
  @Order(0)
  static final SharedKafkaTestResource KAFKA =
      new SharedKafkaTestResource()
          .withBrokerProperty("auto.create.topics.enable", "false")
          // we only need one consumer offsets partition
          .withBrokerProperty("offsets.topic.num.partitions", "1")
          // we don't need to wait that a consumer group rebalances since we always start with a
          // fresh kafka instance
          .withBrokerProperty("group.initial.rebalance.delay.ms", "0")
          .withBrokers(2);

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<KafkaExampleConfiguration> DW =
      new DropwizardAppExtension<>(
          KafkaExampleProducerApplication.class,
          resourceFilePath("test-config-producer.yml"),
          config("kafka.brokers", KAFKA::getKafkaConnectString));

  private static final String TOPIC_NAME = "exampleTopic";

  @Test
  void testUseProducer() throws JsonProcessingException {
    // given
    KafkaExampleProducerApplication application = DW.getApplication();
    // creating topic manually since the createTopicIfMissing method was removed
    KAFKA.getKafkaTestUtils().createTopic(TOPIC_NAME, 1, Short.valueOf("1"));
    final String key = "key";
    final String v1 = "v1";
    final String v2 = "v2";

    // when
    application.sendExample(key, v1, v2);

    // then
    List<ConsumerRecord<byte[], byte[]>> records = new ArrayList<>();
    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              List<ConsumerRecord<byte[], byte[]>> consumerRecords =
                  KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(TOPIC_NAME);
              assertThat(consumerRecords).isNotEmpty();
              records.addAll(consumerRecords);
            });

    assertThat(records)
        .extracting(ConsumerRecord::key)
        .containsExactly(new ObjectMapper().writeValueAsBytes(new Key(key)));

    assertThat(records)
        .extracting(ConsumerRecord::value)
        .containsExactly(new ObjectMapper().writeValueAsBytes(new Value(v1, v2)));
  }

  @Test
  void testUseProducerWithConfiguration() {
    // given
    KafkaExampleProducerApplication application = DW.getApplication();
    // creating topic manually since the createTopicIfMissing method was removed
    KAFKA.getKafkaTestUtils().createTopic("exampleTopicConfiguration", 1, Short.valueOf("1"));

    // when
    application.sendExampleWithConfiguration(1L, 2L);

    // then
    List<ConsumerRecord<byte[], byte[]>> consumerRecords = new ArrayList<>();
    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              List<ConsumerRecord<byte[], byte[]>> consumerRecords =
                  KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic("exampleTopicConfiguration");
              assertThat(consumerRecords).isNotEmpty();
              consumerRecords.addAll(consumerRecords);
            });

    ConsumerRecord<byte[], byte[]> record = consumerRecors.get(0);

    assertThat(getLong(record.key())).isEqualTo(1L);
    assertThat(getLong(record.value())).isEqualTo(2L);
  }

  private static long getLong(byte[] array) {
    return ((long) (array[0] & 0xff) << 56)
        | ((long) (array[1] & 0xff) << 48)
        | ((long) (array[2] & 0xff) << 40)
        | ((long) (array[3] & 0xff) << 32)
        | ((long) (array[4] & 0xff) << 24)
        | ((long) (array[5] & 0xff) << 16)
        | ((long) (array[6] & 0xff) << 8)
        | ((long) (array[7] & 0xff));
  }
}
