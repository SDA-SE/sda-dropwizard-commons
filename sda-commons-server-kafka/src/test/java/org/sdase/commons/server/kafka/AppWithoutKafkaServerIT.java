package org.sdase.commons.server.kafka;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.kafka.builder.MessageListenerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.consumer.IgnoreAndProceedErrorHandler;
import org.sdase.commons.server.kafka.consumer.strategies.autocommit.AutocommitMLS;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;

class AppWithoutKafkaServerIT {

  @RegisterExtension
  public static final DropwizardAppExtension<KafkaTestConfiguration> DW =
      new DropwizardAppExtension<>(
          KafkaTestApplication.class,
          resourceFilePath("test-config-default.yml"),
          config("kafka.brokers", "PLAINTEXT://127.0.0.1:1"));

  private List<String> results = Collections.synchronizedList(new ArrayList<>());

  private KafkaBundle<KafkaTestConfiguration> bundle;

  @BeforeEach
  void before() {
    KafkaTestApplication app = DW.getApplication();
    bundle = app.kafkaBundle();
    results.clear();
  }

  @Test
  void checkMessageListenerCreationThrowsException() {
    String topicName = "checkMessageListenerCreationSuccessful";
    Assertions.assertThrows(
        TimeoutException.class,
        () ->
            bundle.createMessageListener(
                MessageListenerRegistration.builder()
                    .withListenerConfig("lc1")
                    .forTopic(topicName)
                    .checkTopicConfiguration()
                    .withDefaultConsumer()
                    .withValueDeserializer(new StringDeserializer())
                    .withListenerStrategy(
                        new AutocommitMLS<>(
                            record -> results.add(record.value()),
                            new IgnoreAndProceedErrorHandler<>()))
                    .build()));
  }

  @Test
  void checkProducerWithCheckThrowsException() {
    String topicName = "checkProducerWithCreationThrowsException";
    Assertions.assertThrows(
        TimeoutException.class,
        () ->
            bundle.registerProducer(
                ProducerRegistration.builder()
                    .forTopic(topicName)
                    .checkTopicConfiguration()
                    .withDefaultProducer()
                    .build()));
  }
}
