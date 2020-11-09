package org.sdase.commons.server.kafka;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.kafka.builder.MessageListenerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.consumer.IgnoreAndProceedErrorHandler;
import org.sdase.commons.server.kafka.consumer.strategies.autocommit.AutocommitMLS;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;

public class AppWithoutKafkaServerIT {

  @ClassRule
  public static final DropwizardAppRule<KafkaTestConfiguration> DW =
      new DropwizardAppRule<>(
          KafkaTestApplication.class,
          resourceFilePath("test-config-default.yml"),
          config("kafka.brokers", "PLAINTEXT://127.0.0.1:1"));

  private List<String> results = Collections.synchronizedList(new ArrayList<>());

  private KafkaBundle<KafkaTestConfiguration> bundle;

  @Before
  public void before() {
    KafkaTestApplication app = DW.getApplication();
    bundle = app.kafkaBundle();
    results.clear();
  }

  @Test(expected = TimeoutException.class)
  public void checkMessageListenerCreationThrowsException() {
    String topicName = "checkMessageListenerCreationSuccessful";
    bundle.createMessageListener(
        MessageListenerRegistration.builder()
            .withListenerConfig("lc1")
            .forTopic(topicName)
            .checkTopicConfiguration()
            .withDefaultConsumer()
            .withValueDeserializer(new StringDeserializer())
            .withListenerStrategy(
                new AutocommitMLS<>(
                    record -> results.add(record.value()), new IgnoreAndProceedErrorHandler<>()))
            .build());
  }

  @Test(expected = TimeoutException.class)
  public void checkProducerWithCreationThrowsException() {
    String topicName = "checkProducerWithCreationThrowsException";
    bundle.registerProducer(
        ProducerRegistration.builder()
            .forTopic(topicName)
            .createTopicIfMissing()
            .withDefaultProducer()
            .build());
  }

  @Test(expected = TimeoutException.class)
  public void checkProducerWithCheckThrowsException() {
    String topicName = "checkProducerWithCreationThrowsException";
    bundle.registerProducer(
        ProducerRegistration.builder()
            .forTopic(topicName)
            .checkTopicConfiguration()
            .withDefaultProducer()
            .build());
  }
}
