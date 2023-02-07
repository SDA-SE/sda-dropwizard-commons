package org.sdase.commons.server.kafka;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.kafka.builder.MessageListenerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.config.TopicConfig;
import org.sdase.commons.server.kafka.consumer.IgnoreAndProceedErrorHandler;
import org.sdase.commons.server.kafka.consumer.MessageListener;
import org.sdase.commons.server.kafka.consumer.strategies.autocommit.AutocommitMLS;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import org.sdase.commons.server.kafka.producer.MessageProducer;

class KafkaTopicIT {

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
  static final DropwizardAppExtension<KafkaTestConfiguration> DW =
      new DropwizardAppExtension<>(
          KafkaTestApplication.class,
          resourceFilePath("test-config-default.yml"),
          config("kafka.brokers", KAFKA::getKafkaConnectString));

  private KafkaBundle<KafkaTestConfiguration> bundle;
  private final List<String> results = Collections.synchronizedList(new ArrayList<>());

  @BeforeEach
  void setup() {
    results.clear();
    KafkaTestApplication app = DW.getApplication();
    bundle = app.kafkaBundle();
  }

  @Test
  void checkTopicSuccessful() {
    String topicName = "checkTopicSuccessful";
    KAFKA.getKafkaTestUtils().createTopic(topicName, 1, (short) 1);
    List<MessageListener<String, String>> stringStringMessageListener =
        bundle.createMessageListener(
            MessageListenerRegistration.builder()
                .withDefaultListenerConfig()
                .forTopic(topicName)
                .withDefaultConsumer()
                .withValueDeserializer(new StringDeserializer())
                .withListenerStrategy(
                    new AutocommitMLS<String, String>(
                        record -> results.add(record.value()),
                        new IgnoreAndProceedErrorHandler<>()))
                .build());

    assertThat(stringStringMessageListener).isNotNull();
  }

  @Test
  void checkTopicSuccessfulComplex() {
    String topicName = "checkTopicSuccessfulComplex";
    KAFKA.getKafkaTestUtils().createTopic(topicName, 2, (short) 1);
    List<MessageListener<String, String>> stringStringMessageListener =
        bundle.createMessageListener(
            MessageListenerRegistration.builder()
                .withDefaultListenerConfig()
                .forTopicConfigs(
                    Collections.singletonList(TopicConfig.builder().name(topicName).build()))
                .withDefaultConsumer()
                .withValueDeserializer(new StringDeserializer())
                .withListenerStrategy(
                    new AutocommitMLS<String, String>(
                        record -> results.add(record.value()),
                        new IgnoreAndProceedErrorHandler<>()))
                .build());

    assertThat(stringStringMessageListener).isNotNull();
  }

  @Test
  void createSimpleTopic() {
    String topicName = "createSimpleTopic";

    MessageProducer<Object, Object> producer =
        bundle.registerProducer(
            ProducerRegistration.builder()
                .forTopic(TopicConfig.builder().name(topicName).build())
                .withDefaultProducer()
                .build());
    assertThat(producer).isNotNull();
  }

  @Test
  void createSimpleTopicNameOnly() {
    String topicName = "createSimpleTopicNameOnly";
    MessageProducer<Object, Object> producer =
        bundle.registerProducer(
            ProducerRegistration.builder().forTopic(topicName).withDefaultProducer().build());
    assertThat(producer).isNotNull();
  }
}
