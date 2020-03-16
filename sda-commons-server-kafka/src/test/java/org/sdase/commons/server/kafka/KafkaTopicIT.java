package org.sdase.commons.server.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.github.ftrossbach.club_topicana.core.ComparisonResult;
import com.github.ftrossbach.club_topicana.core.ComparisonResult.Comparison;
import com.github.ftrossbach.club_topicana.core.ExpectedTopicConfiguration;
import com.github.ftrossbach.club_topicana.core.MismatchedTopicConfigException;
import com.salesforce.kafka.test.KafkaBroker;
import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.assertj.core.util.Maps;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.consumer.IgnoreAndProceedErrorHandler;
import org.sdase.commons.server.kafka.consumer.MessageListener;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import org.sdase.commons.server.kafka.exception.TopicCreationException;
import org.sdase.commons.server.kafka.producer.MessageProducer;
import org.sdase.commons.server.kafka.topicana.TopicComparer;
import org.sdase.commons.server.kafka.topicana.TopicConfigurationBuilder;
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.sdase.commons.server.testing.LazyRule;

public class KafkaTopicIT {

  private static final SharedKafkaTestResource KAFKA =
      new SharedKafkaTestResource()
          .withBrokerProperty("auto.create.topics.enable", "false")
          // we only need one consumer offsets partition
          .withBrokerProperty("offsets.topic.num.partitions", "1")
          // we don't need to wait that a consumer group rebalances since we always start with a
          // fresh kafka instance
          .withBrokerProperty("group.initial.rebalance.delay.ms", "0")
          .withBrokers(2);

  private static final LazyRule<DropwizardAppRule<KafkaTestConfiguration>> DROPWIZARD_APP_RULE =
      new LazyRule<>(
          () ->
              DropwizardRuleHelper.dropwizardTestAppFrom(KafkaTestApplication.class)
                  .withConfigFrom(KafkaTestConfiguration::new)
                  .withRandomPorts()
                  .withConfigurationModifier(
                      c ->
                          c.getKafka()
                              .setBrokers(
                                  KAFKA.getKafkaBrokers().stream()
                                      .map(KafkaBroker::getConnectString)
                                      .collect(Collectors.toList())))
                  .build());

  @ClassRule
  public static final TestRule CHAIN = RuleChain.outerRule(KAFKA).around(DROPWIZARD_APP_RULE);

  private KafkaBundle<KafkaTestConfiguration> bundle;
  private List<String> results = Collections.synchronizedList(new ArrayList<>());

  @Before
  public void setup() {
    results.clear();
    KafkaTestApplication app = DROPWIZARD_APP_RULE.getRule().getApplication();
    bundle = app.kafkaBundle();
  }

  @Test
  public void checkTopicSuccessful() {
    String topicName = "checkTopicSuccessful";
    KAFKA.getKafkaTestUtils().createTopic(topicName, 1, (short) 1);
    List<MessageListener<String, String>> stringStringMessageListener =
        bundle.registerMessageHandler(
            MessageHandlerRegistration.<String, String>builder()
                .withDefaultListenerConfig()
                .forTopic(topicName)
                .checkTopicConfiguration()
                .withDefaultConsumer()
                .withValueDeserializer(new StringDeserializer())
                .withHandler(record -> results.add(record.value()))
                .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
                .build());

    assertThat(stringStringMessageListener).isNotNull();
  }

  @Test
  public void checkTopicSuccessfulComplex() {
    String topicName = "checkTopicSuccessfulComplex";
    KAFKA.getKafkaTestUtils().createTopic(topicName, 2, (short) 1);
    List<MessageListener<String, String>> stringStringMessageListener =
        bundle.registerMessageHandler(
            MessageHandlerRegistration.<String, String>builder()
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
                .withHandler(record -> results.add(record.value()))
                .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
                .build());

    assertThat(stringStringMessageListener).isNotNull();
  }

  @Test(expected = MismatchedTopicConfigException.class)
  public void checkTopicFails() {

    List<MessageListener<String, String>> stringStringMessageListener =
        bundle.registerMessageHandler(
            MessageHandlerRegistration.<String, String>builder()
                .withDefaultListenerConfig()
                .forTopic("SomeNotExisting")
                .checkTopicConfiguration()
                .withDefaultConsumer()
                .withValueDeserializer(new StringDeserializer())
                .withHandler(record -> results.add(record.value()))
                .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
                .build());

    assertThat(stringStringMessageListener).isNotNull();
  }

  @Test
  public void createSimpleTopic() {
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
  public void createSimpleTopicNameOnly() {
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

  @Test(expected = TopicCreationException.class)
  public void createTopicException() {
    String topicName = "createTopicException";
    ExpectedTopicConfiguration topic =
        TopicConfigurationBuilder.builder(topicName)
            .withPartitionCount(2)
            .withReplicationFactor(2)
            .withConfig("delete.retention.ms", "2000")
            .withConfig("some.bullshit", "2000")
            .build();
    bundle.registerProducer(
        ProducerRegistration.builder()
            .forTopic(topic)
            .createTopicIfMissing()
            .withDefaultProducer()
            .build());
  }

  @Test(expected = MismatchedTopicConfigException.class)
  public void compareTopicWithConfigFail() {
    String topicName = "compareTopicWithConfigFail";
    createTopic(topicName, Maps.newHashMap("delete.retention.ms", "9999"));

    ExpectedTopicConfiguration topic =
        TopicConfigurationBuilder.builder(topicName)
            .withPartitionCount(2)
            .withReplicationFactor(2)
            .withConfig("delete.retention.ms", "2000")
            .build();
    bundle.registerProducer(
        ProducerRegistration.builder()
            .forTopic(topic)
            .checkTopicConfiguration()
            .withDefaultProducer()
            .build());
  }

  @Test
  public void compareTopicWrongProperty() {
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
        comparer.compare(
            Collections.singletonList(topic),
            DROPWIZARD_APP_RULE.getRule().getConfiguration().getKafka());

    assertThat(compare).isNotNull();
    Collection<Comparison<String>> topicCompareDetails =
        compare.getMismatchingConfiguration().get(topicName);
    assertThat(topicCompareDetails).hasSize(1);
    assertThat(topicCompareDetails.toArray(new ComparisonResult.Comparison[] {})[0])
        .extracting("property", "actualValue", "expectedValue")
        .containsExactly("delete.retention.ms", "9999", "2000");
  }

  @Test
  public void compareTopicMissing() {
    String topicName = "compareTopicMissing";

    TopicComparer comparer = new TopicComparer();
    ExpectedTopicConfiguration topic =
        TopicConfigurationBuilder.builder(topicName)
            .withPartitionCount(2)
            .withReplicationFactor(2)
            .build();
    ComparisonResult compare =
        comparer.compare(
            Collections.singletonList(topic),
            DROPWIZARD_APP_RULE.getRule().getConfiguration().getKafka());

    assertThat(compare).isNotNull();
    assertThat(compare.getMissingTopics()).containsExactly(topicName);
  }

  @Test
  public void compareTopicWrongReplication() {
    String topicName = "compareTopicWrongReplication";

    createTopic(topicName);

    TopicComparer comparer = new TopicComparer();
    ExpectedTopicConfiguration topic =
        TopicConfigurationBuilder.builder(topicName)
            .withPartitionCount(1)
            .withReplicationFactor(1)
            .build();

    ComparisonResult compare =
        comparer.compare(
            Collections.singletonList(topic),
            DROPWIZARD_APP_RULE.getRule().getConfiguration().getKafka());

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
  public void compareTopicWithConfigOk() {
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
  public void createComplexTopic() {
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
