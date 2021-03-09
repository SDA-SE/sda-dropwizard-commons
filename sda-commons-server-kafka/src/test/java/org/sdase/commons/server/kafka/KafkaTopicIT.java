package org.sdase.commons.server.kafka;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;

import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.kafka.builder.MessageListenerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.consumer.IgnoreAndProceedErrorHandler;
import org.sdase.commons.server.kafka.consumer.MessageListener;
import org.sdase.commons.server.kafka.consumer.strategies.autocommit.AutocommitMLS;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import org.sdase.commons.server.kafka.exception.TopicCreationException;
import org.sdase.commons.server.kafka.producer.MessageProducer;
import org.sdase.commons.server.kafka.topicana.ComparisonResult;
import org.sdase.commons.server.kafka.topicana.ComparisonResult.Comparison;
import org.sdase.commons.server.kafka.topicana.ExpectedTopicConfiguration;
import org.sdase.commons.server.kafka.topicana.MismatchedTopicConfigException;
import org.sdase.commons.server.kafka.topicana.TopicComparer;
import org.sdase.commons.server.kafka.topicana.TopicConfigurationBuilder;

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
  public void setup() {
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
                .checkTopicConfiguration()
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
                    Collections.singletonList(
                        TopicConfigurationBuilder.builder(topicName)
                            .withPartitionCount(2)
                            .withReplicationFactor(1)
                            .build()))
                .checkTopicConfiguration()
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
  void checkTopicFails() {
    assertThatCode(
            () ->
                bundle.createMessageListener(
                    MessageListenerRegistration.builder()
                        .withDefaultListenerConfig()
                        .forTopic("SomeNotExisting")
                        .checkTopicConfiguration()
                        .withDefaultConsumer()
                        .withValueDeserializer(new StringDeserializer())
                        .withListenerStrategy(
                            new AutocommitMLS<String, String>(
                                record -> results.add(record.value()),
                                new IgnoreAndProceedErrorHandler<>()))
                        .build()))
        .isInstanceOf(MismatchedTopicConfigException.class);
  }

  @Test
  void createSimpleTopic() {
    String topicName = "createSimpleTopic";
    ExpectedTopicConfiguration topic =
        TopicConfigurationBuilder.builder(topicName)
            .withPartitionCount(2)
            .withReplicationFactor(2)
            .build();

    MessageProducer<Object, Object> producer =
        bundle.registerProducer(
            ProducerRegistration.builder()
                .forTopic(topic)
                .createTopicIfMissing()
                .withDefaultProducer()
                .build());
    assertThat(producer).isNotNull();
  }

  @Test
  void createSimpleTopicNameOnly() {
    String topicName = "createSimpleTopicNameOnly";
    MessageProducer<Object, Object> producer =
        bundle.registerProducer(
            ProducerRegistration.builder()
                .forTopic(topicName)
                .createTopicIfMissing()
                .withDefaultProducer()
                .build());
    assertThat(producer).isNotNull();
  }

  @Test
  void createTopicException() {
    String topicName = "createTopicException";
    ExpectedTopicConfiguration topic =
        TopicConfigurationBuilder.builder(topicName)
            .withPartitionCount(2)
            .withReplicationFactor(2)
            .withConfig("delete.retention.ms", "2000")
            .withConfig("some.bullshit", "2000")
            .build();
    assertThatCode(
            () ->
                bundle.registerProducer(
                    ProducerRegistration.builder()
                        .forTopic(topic)
                        .createTopicIfMissing()
                        .withDefaultProducer()
                        .build()))
        .isInstanceOf(TopicCreationException.class);
  }

  @Test
  void compareTopicWithConfigFail() {
    String topicName = "compareTopicWithConfigFail";
    createTopic(topicName, Maps.newHashMap("delete.retention.ms", "9999"));

    ExpectedTopicConfiguration topic =
        TopicConfigurationBuilder.builder(topicName)
            .withPartitionCount(2)
            .withReplicationFactor(2)
            .withConfig("delete.retention.ms", "2000")
            .build();
    assertThatCode(
            () ->
                bundle.registerProducer(
                    ProducerRegistration.builder()
                        .forTopic(topic)
                        .checkTopicConfiguration()
                        .withDefaultProducer()
                        .build()))
        .isInstanceOf(MismatchedTopicConfigException.class);
  }

  @Test
  void compareTopicWrongProperty() {
    String topicName = "compareTopicWrongProperty";
    createTopic(topicName, Maps.newHashMap("delete.retention.ms", "9999"));

    TopicComparer comparer = new TopicComparer();
    ExpectedTopicConfiguration topic =
        TopicConfigurationBuilder.builder(topicName)
            .withPartitionCount(2)
            .withReplicationFactor(2)
            .withConfig("delete.retention.ms", "2000")
            .build();
    ComparisonResult compare =
        comparer.compare(Collections.singletonList(topic), DW.getConfiguration().getKafka());

    assertThat(compare).isNotNull();
    Collection<Comparison<String>> topicCompareDetails =
        compare.getMismatchingConfiguration().get(topicName);
    assertThat(topicCompareDetails).hasSize(1);
    assertThat(topicCompareDetails.toArray(new ComparisonResult.Comparison[] {})[0])
        .extracting("property", "actualValue", "expectedValue")
        .containsExactly("delete.retention.ms", "9999", "2000");
  }

  @Test
  void compareTopicMissing() {
    String topicName = "compareTopicMissing";

    TopicComparer comparer = new TopicComparer();
    ExpectedTopicConfiguration topic =
        TopicConfigurationBuilder.builder(topicName)
            .withPartitionCount(2)
            .withReplicationFactor(2)
            .build();
    ComparisonResult compare =
        comparer.compare(Collections.singletonList(topic), DW.getConfiguration().getKafka());

    assertThat(compare).isNotNull();
    assertThat(compare.getMissingTopics()).containsExactly(topicName);
  }

  @Test
  void compareTopicWrongReplication() {
    String topicName = "compareTopicWrongReplication";

    createTopic(topicName);

    TopicComparer comparer = new TopicComparer();
    ExpectedTopicConfiguration topic =
        TopicConfigurationBuilder.builder(topicName)
            .withPartitionCount(1)
            .withReplicationFactor(1)
            .build();

    ComparisonResult compare =
        comparer.compare(Collections.singletonList(topic), DW.getConfiguration().getKafka());

    assertThat(compare).isNotNull();
    assertThat(compare.getMismatchingReplicationFactor().get(topicName))
        .extracting("property", "actualValue", "expectedValue")
        .containsExactly("replication factor", 2, 1);
    assertThat(compare.getMismatchingPartitionCount().get(topicName))
        .extracting("property", "actualValue", "expectedValue")
        .containsExactly("partition count", 2, 1);
  }

  private void createTopic(String topicName, Map<String, String> properties) {
    try (AdminClient admin = KAFKA.getKafkaTestUtils().getAdminClient()) {
      NewTopic newTopic = new NewTopic(topicName, 2, (short) 2).configs(properties);
      KafkaFuture<Void> result = admin.createTopics(Collections.singletonList(newTopic)).all();
      await().until(result::isDone);
    }
  }

  private void createTopic(String topicName) {
    createTopic(topicName, null);
  }

  @Test
  void compareTopicWithConfigOk() {
    String topicName = "compareTopicWithConfigOk";

    try (AdminClient admin = KAFKA.getKafkaTestUtils().getAdminClient()) {
      NewTopic newTopic = new NewTopic(topicName, 2, (short) 2);
      newTopic.configs(Maps.newHashMap("delete.retention.ms", "9999"));
      KafkaFuture<Void> result = admin.createTopics(Collections.singletonList(newTopic)).all();

      await().atMost(5, TimeUnit.SECONDS).until(result::isDone);
    }

    ExpectedTopicConfiguration topic =
        TopicConfigurationBuilder.builder(topicName)
            .withPartitionCount(2)
            .withReplicationFactor(2)
            .withConfig("delete.retention.ms", "9999")
            .build();
    bundle.registerProducer(
        ProducerRegistration.builder()
            .forTopic(topic)
            .checkTopicConfiguration()
            .withDefaultProducer()
            .build());
  }

  @Test
  void createComplexTopic() {
    String topicName = "createComplexTopic";
    ExpectedTopicConfiguration topic =
        TopicConfigurationBuilder.builder(topicName)
            .withPartitionCount(2)
            .withReplicationFactor(2)
            .withConfig("delete.retention.ms", "2000")
            .withConfig("cleanup.policy", "delete")
            .build();
    MessageProducer<Object, Object> producer =
        bundle.registerProducer(
            ProducerRegistration.builder()
                .forTopic(topic)
                .createTopicIfMissing()
                .withDefaultProducer()
                .build());
    assertThat(producer).isNotNull();
  }
}
