package org.sdase.commons.server.kafka;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.sdase.commons.server.dropwizard.metadata.DetachedMetadataContext;
import org.sdase.commons.server.dropwizard.metadata.MetadataContext;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import org.sdase.commons.server.kafka.producer.MessageProducer;

@SetSystemProperty(key = "METADATA_FIELDS", value = "tenant-id")
class KafkaBundleMetadataContextProducerIntegrationTest {

  @RegisterExtension
  @Order(0)
  static final SharedKafkaTestResource KAFKA = new SharedKafkaTestResource();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<KafkaTestConfiguration> DROPWIZARD_APP_EXTENSION =
      new DropwizardAppExtension<>(
          KafkaTestApplication.class,
          resourceFilePath("test-config-default.yml"),
          config("kafka.brokers", KAFKA::getKafkaConnectString),

          // performance improvements in the tests
          config("kafka.config.heartbeat\\.interval\\.ms", "250"),
          config("kafka.adminConfig.adminClientRequestTimeoutMs", "30000"));

  private static final String TOPIC = "metadata-producer";

  private int messagesBefore = 0;

  private MessageProducer<String, String> kafkaProducer;

  @BeforeAll
  static void createTopic() {
    KAFKA.getKafkaTestUtils().createTopic(TOPIC, 1, (short) 1);
  }

  @BeforeEach
  void before() {
    messagesBefore = KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(TOPIC).size();

    KafkaTestApplication app = DROPWIZARD_APP_EXTENSION.getApplication();
    KafkaBundle<KafkaTestConfiguration> kafkaBundle = app.kafkaBundle();

    ProducerRegistration<String, String> producerRegistration =
        ProducerRegistration.builder()
            .forTopic(TOPIC)
            .withDefaultProducer()
            .withKeySerializer(new StringSerializer())
            .withValueSerializer(new StringSerializer())
            .build();
    kafkaProducer = kafkaBundle.registerProducer(producerRegistration);
  }

  @BeforeEach
  @AfterEach
  void clearMetadata() {
    MetadataContext.createContext(new DetachedMetadataContext());
  }

  @Test
  void shouldSendMetadataContext() {
    DetachedMetadataContext given = new DetachedMetadataContext();
    given.put("tenant-id", List.of("t-1"));
    MetadataContext.createContext(given);

    kafkaProducer.send("key", "value");

    await()
        .untilAsserted(
            () -> {
              List<ConsumerRecord<byte[], byte[]>> consumerRecords =
                  KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(TOPIC);
              assertThat(consumerRecords).hasSize(messagesBefore + 1);
              assertThat(consumerRecords.get(consumerRecords.size() - 1).headers())
                  .extracting(Header::key, Header::value)
                  .contains(tuple("tenant-id", "t-1".getBytes(StandardCharsets.UTF_8)));
            });
  }

  @Test
  void shouldKeepDefinedHeaders() {
    DetachedMetadataContext given = new DetachedMetadataContext();
    given.put("tenant-id", List.of("t-1"));
    MetadataContext.createContext(given);
    var headers = new RecordHeaders();
    headers.add("custom", "custom-value".getBytes(StandardCharsets.UTF_8));

    kafkaProducer.send("key", "value", headers);

    await()
        .untilAsserted(
            () -> {
              List<ConsumerRecord<byte[], byte[]>> consumerRecords =
                  KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(TOPIC);
              assertThat(consumerRecords).hasSize(messagesBefore + 1);
              assertThat(consumerRecords.get(consumerRecords.size() - 1).headers())
                  .extracting(Header::key, Header::value)
                  .contains(
                      tuple("tenant-id", "t-1".getBytes(StandardCharsets.UTF_8)),
                      tuple("custom", "custom-value".getBytes(StandardCharsets.UTF_8)));
            });
  }

  @Test
  void shouldPreferHeadersFromMetadataContext() {
    DetachedMetadataContext given = new DetachedMetadataContext();
    given.put("tenant-id", List.of("t-1"));
    MetadataContext.createContext(given);
    var headers = new RecordHeaders();
    headers.add("tenant-id", "custom-tenant".getBytes(StandardCharsets.UTF_8));

    kafkaProducer.send("key", "value", headers);

    await()
        .untilAsserted(
            () -> {
              List<ConsumerRecord<byte[], byte[]>> consumerRecords =
                  KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(TOPIC);
              assertThat(consumerRecords).hasSize(messagesBefore + 1);
              assertThat(consumerRecords.get(consumerRecords.size() - 1).headers())
                  .extracting(Header::key, Header::value)
                  .contains(tuple("tenant-id", "t-1".getBytes(StandardCharsets.UTF_8)));
            });
  }

  @Test
  void shouldSendMetadataContextNormalized() {
    DetachedMetadataContext given = new DetachedMetadataContext();
    given.put("tenant-id", List.of("t-1", "  ", " t-2 "));
    MetadataContext.createContext(given);

    kafkaProducer.send("key", "value");

    await()
        .untilAsserted(
            () -> {
              List<ConsumerRecord<byte[], byte[]>> consumerRecords =
                  KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(TOPIC);
              assertThat(consumerRecords).hasSize(messagesBefore + 1);
              assertThat(consumerRecords.get(consumerRecords.size() - 1).headers())
                  .extracting(Header::key, Header::value)
                  .contains(
                      tuple("tenant-id", "t-1".getBytes(StandardCharsets.UTF_8)),
                      tuple("tenant-id", "t-2".getBytes(StandardCharsets.UTF_8)));
            });
  }

  @Test
  void shouldNotSendUnknownFieldsOfMetadataContext() {
    DetachedMetadataContext given = new DetachedMetadataContext();
    given.put("shop-id", List.of("s-1"));
    MetadataContext.createContext(given);

    kafkaProducer.send("key", "value");

    await()
        .untilAsserted(
            () -> {
              List<ConsumerRecord<byte[], byte[]>> consumerRecords =
                  KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(TOPIC);
              assertThat(consumerRecords).hasSize(messagesBefore + 1);
              assertThat(consumerRecords.get(consumerRecords.size() - 1).headers())
                  .extracting(Header::key, Header::value)
                  .doesNotContain(tuple("shop-id", "s-1".getBytes(StandardCharsets.UTF_8)));
            });
  }
}
