package org.example;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.awaitility.Awaitility.await;

import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.jackson.ObjectMapperConfigurationUtil;

class KafkaDoubleAppTest {

  static final String SOURCE_TOPIC_NAME = "source-test";
  static final String TARGET_TOPIC_NAME = "target-test";

  @RegisterExtension
  @Order(0)
  static final SharedKafkaTestResource SOURCE_KAFKA = new SharedKafkaTestResource();

  @RegisterExtension
  @Order(0)
  static final SharedKafkaTestResource TARGET_KAFKA = new SharedKafkaTestResource();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<KafkaDoubleApp.Config> DW =
      new DropwizardAppExtension<>(
          KafkaDoubleApp.class,
          null,
          randomPorts(),
          config("kafkaSource.brokers", SOURCE_KAFKA::getKafkaConnectString),
          config("kafkaSource.topics.source-topic.name", SOURCE_TOPIC_NAME),
          config("kafkaTarget.brokers", TARGET_KAFKA::getKafkaConnectString),
          config("kafkaTarget.topics.target-topic.name", TARGET_TOPIC_NAME));

  @BeforeAll
  static void beforeAll() {
    SOURCE_KAFKA.getKafkaTestUtils().createTopic(SOURCE_TOPIC_NAME, 1, (short) 1);
    TARGET_KAFKA.getKafkaTestUtils().createTopic(TARGET_TOPIC_NAME, 1, (short) 1);
  }

  @Test
  void shouldReadFromSourceAndWriteToTarget() {
    SOURCE_KAFKA
        .getKafkaTestUtils()
        .produceRecords(
            List.of(
                new ProducerRecord<>(
                    SOURCE_TOPIC_NAME,
                    "TEST-KEY".getBytes(UTF_8),
                    """
                    {"number": 2}
                    """
                        .getBytes(UTF_8))));
    await()
        .untilAsserted(
            () ->
                assertThat(
                        TARGET_KAFKA
                            .getKafkaTestUtils()
                            .consumeAllRecordsFromTopic(TARGET_TOPIC_NAME))
                    .hasSize(1)
                    .first()
                    .extracting(this::extractValueNumber)
                    .isEqualTo(4));
  }

  @Test
  void shouldProvideHealthChecks() {
    try (var health =
        DW.client()
            .target("http://localhost:%d".formatted(DW.getAdminPort()))
            .path("healthcheck")
            .path("internal")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get()) {
      Object healthBody = health.readEntity(Object.class);
      assertThat(healthBody)
          .asInstanceOf(MAP)
          .containsKeys("kafkaSourceBroker", "kafkaTargetBroker")
          .doesNotContainKeys("kafkaConnection", "kafkaConnectionExternal");
    }
  }

  private Integer extractValueNumber(ConsumerRecord<byte[], byte[]> cr) {
    try {
      var v = cr.value();
      return ObjectMapperConfigurationUtil.configureMapper()
          .build()
          .readValue(v, KafkaDoubleApp.Message.class)
          .number();
    } catch (IOException e) {
      return null;
    }
  }
}
