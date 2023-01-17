package org.sdase.commons.server.kafka.producer;

import static org.assertj.core.api.Assertions.assertThat;

import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import java.util.Arrays;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;
import org.sdase.commons.server.kafka.KafkaConfiguration;
import org.sdase.commons.server.kafka.KafkaProperties;
import org.sdase.commons.server.kafka.prometheus.ProducerTopicMessageCounter;

class KafkaMessageProducerIT {

  @RegisterExtension
  static final SharedKafkaTestResource KAFKA =
      new SharedKafkaTestResource()
          .withBrokerProperty("offsets.topic.num.partitions", "1")
          // a broker with limited accepted message size
          .withBrokerProperty("message.max.bytes", "100")
          .withBrokerProperty("max.message.bytes", "100");

  static KafkaMessageProducer<String, String> testee;
  private static final String topicName = "test-topic";
  private static ProducerTopicMessageCounter messageCounter;
  private static KafkaProducer<String, String> producer;

  @BeforeAll
  static void beforeAll() {
    messageCounter = new ProducerTopicMessageCounter();
    KafkaProperties producerProperties =
        KafkaProperties.forProducer(
            new KafkaConfiguration()
                .setBrokers(Arrays.asList(KAFKA.getKafkaConnectString().split(","))));
    producer =
        new KafkaProducer<>(producerProperties, new StringSerializer(), new StringSerializer());
    testee = new KafkaMessageProducer(topicName, producer, messageCounter, "test-producer");
  }

  @AfterAll
  static void afterAll() {
    messageCounter.unregister();
    producer.close();
  }

  @Test
  @StdIo
  void shouldLogAndDelegate(StdOut out) {
    // given
    String statement = "A message has been produced.";
    // a dummy callback to run on completion
    Callback callback =
        (metadata, exception) -> {
          System.out.println(statement);
        };

    // when
    testee.send(
        "k",
        "3nC8Vd2BE1ZHEnpozFmE7Jay5uwajvtFHI6lkD06UWW3xwYNkL2y3tYNLfa8AxPj9SdUo4r5XisrWYxVMs8mtrfrCOet3Ble56silErvLtAATxgfaiuSF6lNhoHDkqOc9VgJeuZfcSdxfTQcGWmyY5AxJdk5OQnIxUj3JsKWyoFTzzcTNWRMrO1u5JSIcNJmodhuiCoQ",
        null,
        callback);

    // then
    Awaitility.await()
        .untilAsserted(
            () -> {
              assertThat(out.capturedLines())
                  .anyMatch(l -> l.contains(statement))
                  .anyMatch(
                      l ->
                          l.contains(
                              "An error occurred while producing a message with key k to the topic test-topic."));
            });
  }

  @Test
  @StdIo
  void shouldLogErrors(StdOut out) {
    // given large test value not supported by the broker
    // when
    testee.send(
        "k",
        "3nC8Vd2BE1ZHEnpozFmE7Jay5uwajvtFHI6lkD06UWW3xwYNkL2y3tYNLfa8AxPj9SdUo4r5XisrWYxVMs8mtrfrCOet3Ble56silErvLtAATxgfaiuSF6lNhoHDkqOc9VgJeuZfcSdxfTQcGWmyY5AxJdk5OQnIxUj3JsKWyoFTzzcTNWRMrO1u5JSIcNJmodhuiCoQ");

    // then
    Awaitility.await()
        .untilAsserted(
            () -> {
              assertThat(out.capturedLines())
                  .anyMatch(
                      l ->
                          l.contains(
                              "An error occurred while producing a message with key k to the topic test-topic."))
                  .anyMatch(
                      l -> l.contains("org.apache.kafka.common.errors.RecordTooLargeException"));
            });
  }
}
