package org.sdase.commons.server.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sdase.commons.server.kafka.confluent.testing.KafkaBrokerEnvironmentRule;
import org.sdase.commons.server.kafka.model.Key;
import org.sdase.commons.server.kafka.model.Value;

public class KafkaExampleProducerIT {

  private static final SharedKafkaTestResource KAFKA =
      new SharedKafkaTestResource()
          .withBrokerProperty("auto.create.topics.enable", "false")
          .withBrokers(2);

  private static final KafkaBrokerEnvironmentRule KAFKA_BROKER_ENVIRONMENT_RULE =
      new KafkaBrokerEnvironmentRule(KAFKA);

  private static final DropwizardAppRule<KafkaExampleConfiguration> DROPWIZARD_APP_RULE =
      new DropwizardAppRule<>(
          KafkaExampleProducerApplication.class,
          ResourceHelpers.resourceFilePath("test-config-producer.yml"));

  @ClassRule
  public static final TestRule CHAIN =
      RuleChain.outerRule(KAFKA_BROKER_ENVIRONMENT_RULE).around(DROPWIZARD_APP_RULE);

  private static final String TOPIC_NAME = "exampleTopic";

  @Test
  public void testUseProducer() throws JsonProcessingException {
    // given
    KafkaExampleProducerApplication application = DROPWIZARD_APP_RULE.getApplication();
    final String key = "key";
    final String v1 = "v1";
    final String v2 = "v2";

    // when
    application.sendExample(key, v1, v2);

    // then
    await()
        .atMost(10, TimeUnit.SECONDS)
        .until(() -> !KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(TOPIC_NAME).isEmpty());

    assertThat(KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(TOPIC_NAME))
        .extracting(ConsumerRecord::key)
        .containsExactly(new ObjectMapper().writeValueAsBytes(new Key(key)));

    assertThat(KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(TOPIC_NAME))
        .extracting(ConsumerRecord::value)
        .containsExactly(new ObjectMapper().writeValueAsBytes(new Value(v1, v2)));
  }

  @Test
  public void testUseProducerWithConfiguration() {
    // given
    KafkaExampleProducerApplication application = DROPWIZARD_APP_RULE.getApplication();

    // when
    application.sendExampleWithConfiguration(1L, 2L);

    // then
    await()
        .atMost(10, TimeUnit.SECONDS)
        .until(
            () ->
                !KAFKA
                    .getKafkaTestUtils()
                    .consumeAllRecordsFromTopic("exampleTopicConfiguration")
                    .isEmpty());

    ConsumerRecord<byte[], byte[]> record =
        KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic("exampleTopicConfiguration").get(0);

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
