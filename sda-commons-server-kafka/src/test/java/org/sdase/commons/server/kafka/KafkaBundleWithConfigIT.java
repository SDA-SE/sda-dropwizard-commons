package org.sdase.commons.server.kafka;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesforce.kafka.test.KafkaBroker;
import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import org.sdase.commons.server.kafka.builder.MessageListenerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.config.AdminConfig;
import org.sdase.commons.server.kafka.config.ConsumerConfig;
import org.sdase.commons.server.kafka.config.ListenerConfig;
import org.sdase.commons.server.kafka.config.ProducerConfig;
import org.sdase.commons.server.kafka.config.TopicConfig;
import org.sdase.commons.server.kafka.consumer.ErrorHandler;
import org.sdase.commons.server.kafka.consumer.IgnoreAndProceedErrorHandler;
import org.sdase.commons.server.kafka.consumer.KafkaHelper;
import org.sdase.commons.server.kafka.consumer.MessageHandler;
import org.sdase.commons.server.kafka.consumer.MessageListener;
import org.sdase.commons.server.kafka.consumer.strategies.autocommit.AutocommitMLS;
import org.sdase.commons.server.kafka.consumer.strategies.legacy.CallbackMessageHandler;
import org.sdase.commons.server.kafka.consumer.strategies.legacy.LegacyMLS;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import org.sdase.commons.server.kafka.exception.ConfigurationException;
import org.sdase.commons.server.kafka.producer.MessageProducer;
import org.sdase.commons.server.kafka.serializers.KafkaJsonDeserializer;
import org.sdase.commons.server.kafka.serializers.KafkaJsonSerializer;
import org.sdase.commons.server.kafka.serializers.SimpleEntity;
import org.sdase.commons.server.kafka.serializers.WrappedNoSerializationErrorDeserializer;
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.sdase.commons.server.testing.LazyRule;

public class KafkaBundleWithConfigIT {

  private static final SharedKafkaTestResource KAFKA =
      new SharedKafkaTestResource()
          // we only need one consumer offsets partition
          .withBrokerProperty("offsets.topic.num.partitions", "1")
          // we don't need to wait that a consumer group rebalances since we always start with a
          // fresh kafka instance
          .withBrokerProperty("group.initial.rebalance.delay.ms", "0");

  private static final String CONSUMER_1 = "consumer1";
  private static final String PRODUCER_1 = "producer1";
  private static final String CONSUMER_2 = "consumer2";
  private static final String PRODUCER_2 = "producer2";

  private static final String LISTENER_CONFIG_ASYNC = "async";
  private static final String CLIENT_ID_ASYNC = "async";

  private static final LazyRule<DropwizardAppRule<KafkaTestConfiguration>> DROPWIZARD_APP_RULE =
      new LazyRule<>(
          () ->
              DropwizardRuleHelper.dropwizardTestAppFrom(KafkaTestApplication.class)
                  .withConfigFrom(KafkaTestConfiguration::new)
                  .withRandomPorts()
                  .withConfigurationModifier(
                      c -> {
                        KafkaConfiguration kafka = c.getKafka();

                        kafka.setBrokers(
                            KAFKA.getKafkaBrokers().stream()
                                .map(KafkaBroker::getConnectString)
                                .collect(Collectors.toList()));

                        AdminConfig adminConfig = new AdminConfig();
                        adminConfig.setAdminClientRequestTimeoutMs(2000);
                        kafka.setAdminConfig(adminConfig);

                        kafka
                            .getConsumers()
                            .put(
                                CONSUMER_1,
                                ConsumerConfig.builder()
                                    .withGroup("default")
                                    .withClientId(CONSUMER_1)
                                    .addConfig(
                                        "key.deserializer",
                                        "org.apache.kafka.common.serialization.LongDeserializer")
                                    .addConfig(
                                        "value.deserializer",
                                        "org.apache.kafka.common.serialization.LongDeserializer")
                                    .build());
                        kafka
                            .getConsumers()
                            .put(CONSUMER_2, ConsumerConfig.builder().withClientId("c2").build());

                        kafka
                            .getProducers()
                            .put(
                                PRODUCER_1,
                                ProducerConfig.builder()
                                    .addConfig(
                                        "key.serializer",
                                        "org.apache.kafka.common.serialization.LongSerializer")
                                    .addConfig(
                                        "value.serializer",
                                        "org.apache.kafka.common.serialization.LongSerializer")
                                    .build());
                        kafka
                            .getProducers()
                            .put(PRODUCER_2, ProducerConfig.builder().withClientId("p2").build());

                        kafka
                            .getListenerConfig()
                            .put(
                                LISTENER_CONFIG_ASYNC,
                                ListenerConfig.builder()
                                    .withCommitType(LegacyMLS.CommitType.ASYNC) // NOSONAR
                                    .useAutoCommitOnly(false) // NOSONAR
                                    .withTopicMissingRetryMs(60000)
                                    .build(1));

                        kafka
                            .getTopics()
                            .put(
                                "topicId1",
                                TopicConfig.builder()
                                    .name("topic1")
                                    .withPartitions(2)
                                    .withReplicationFactor(2)
                                    .addConfig("max.message.bytes", "1024")
                                    .addConfig("retention.ms", "60480000")
                                    .build());
                        kafka
                            .getTopics()
                            .put(
                                "topicId2", // NOSONAR
                                TopicConfig.builder()
                                    .name("topic2")
                                    .withPartitions(1)
                                    .withReplicationFactor(1)
                                    .build()); // NOSONAR
                      })
                  .build());

  @ClassRule
  public static final TestRule CHAIN = RuleChain.outerRule(KAFKA).around(DROPWIZARD_APP_RULE);

  private List<Long> results = Collections.synchronizedList(new ArrayList<>());
  private List<String> resultsString = Collections.synchronizedList(new ArrayList<>());

  private KafkaBundle<KafkaTestConfiguration> kafkaBundle;
  private int callbackCount = 0;

  private static KafkaProducer<String, String> stringStringProducer;

  @BeforeClass
  public static void beforeClass() {
    stringStringProducer =
        KAFKA.getKafkaTestUtils().getKafkaProducer(StringSerializer.class, StringSerializer.class);
  }

  @AfterClass
  public static void afterClass() {
    stringStringProducer.close();
  }

  @Before
  public void before() {
    KafkaTestApplication app = DROPWIZARD_APP_RULE.getRule().getApplication();
    kafkaBundle = app.kafkaBundle();
    results.clear();
    resultsString.clear();
  }

  @Test
  public void healthCheckShouldBeAdded() {
    KafkaTestApplication app = DROPWIZARD_APP_RULE.getRule().getApplication();
    assertThat(app.healthCheckRegistry().getHealthCheck("kafkaConnection")).isNotNull();
  }

  @Test
  public void allTopicsDescriptionsGenerated() {
    final String testTopic1 = "topicId1";
    assertThat(kafkaBundle.getTopicConfiguration(testTopic1)).isNotNull();
    assertThat(kafkaBundle.getTopicConfiguration(testTopic1).getReplicationFactor().count())
        .isEqualTo(2);
    assertThat(kafkaBundle.getTopicConfiguration(testTopic1).getPartitions().count()).isEqualTo(2);
    assertThat(kafkaBundle.getTopicConfiguration(testTopic1).getProps()).hasSize(2);
    assertThat(kafkaBundle.getTopicConfiguration("topicId2")).isNotNull();
  }

  @Test
  public void createProducerWithTopic() {
    MessageProducer<String, String> topicName2 =
        kafkaBundle.registerProducer(
            ProducerRegistration.<String, String>builder()
                .forTopic(kafkaBundle.getTopicConfiguration("topicId2"))
                .createTopicIfMissing()
                .withDefaultProducer()
                .withValueSerializer(new StringSerializer())
                .build());
    assertThat(topicName2).isNotNull();
  }

  @Test
  public void autocommitStrategyShouldCommit() {
    String topic = "autocommitStrategyShouldCommit";
    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);
    AtomicLong offset = new AtomicLong(0);

    MessageHandler<String, String> handler =
        record -> {
          throw new ProcessingRecordException("Something wrong");
        };
    ErrorHandler<String, String> errorHandler =
        (record, e, consumer) -> {
          TopicPartition topicPartition = new TopicPartition(topic, record.partition());
          offset.set(
              consumer.endOffsets(Collections.singletonList(topicPartition)).get(topicPartition));
          return true;
        };

    kafkaBundle.createMessageListener(
        MessageListenerRegistration.<String, String>builder() // NOSONAR
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withConsumerConfig(
                ConsumerConfig.builder()
                    .addConfig("max.poll.records", "1")
                    .addConfig("auto.commit.interval.ms", "1")
                    .build())
            .withValueDeserializer(new StringDeserializer())
            .withListenerStrategy(new AutocommitMLS<>(handler, errorHandler))
            .build());

    for (int i = 0; i < KafkaBundleConsts.N_MESSAGES; i++) {
      String message = UUID.randomUUID().toString();
      stringStringProducer.send(new ProducerRecord<>(topic, message));
    }

    await()
        .atLeast(100, MILLISECONDS)
        .untilAsserted(() -> assertThat(offset.get()).isGreaterThanOrEqualTo(5L));
  }

  @Test(expected = ConfigurationException.class)
  public void shouldProduceConfigExceptionWhenConsumerConfigNotExists() {
    try (KafkaConsumer<String, String> consumer =
        kafkaBundle.createConsumer(
            new StringDeserializer(), new StringDeserializer(), "notExistingConsumerConfig")) {
      // empty
    }
  }

  @Test
  public void shouldReturnConsumerByConsumerConfigName() {
    try (KafkaConsumer<String, String> consumer =
        kafkaBundle.createConsumer(
            new StringDeserializer(), new StringDeserializer(), CONSUMER_1)) {
      assertThat(consumer).isNotNull();
      assertThat(KafkaHelper.getClientId(consumer)).isEqualTo(CONSUMER_1 + "-0");
    }
  }

  @Test
  public void shouldReturnConsumerByConsumerConfig() {
    try (KafkaConsumer<String, String> consumer =
        kafkaBundle.createConsumer(
            new StringDeserializer(),
            new StringDeserializer(),
            ConsumerConfig.builder()
                .withGroup("test-consumer")
                .withClientId("puff-the-magic-consumer")
                .addConfig("max.poll.records", "10")
                .addConfig("enable.auto.commit", "false")
                .build(),
            1)) {
      assertThat(consumer).isNotNull();
      assertThat(KafkaHelper.getClientId(consumer)).isEqualTo("puff-the-magic-consumer-1");
    }
  }

  @Test(expected = ConfigurationException.class)
  public void shouldProduceConfigExceptionWhenProducerConfigNotExists() {
    try (KafkaProducer<String, String> producer =
        kafkaBundle.createProducer(
            new StringSerializer(), new StringSerializer(), "notExistingProducerConfig")) {
      // empty
    }
  }

  @Test
  public void shouldReturnProducerByProducerConfigName() {
    try (KafkaProducer<String, String> producer =
        kafkaBundle.createProducer(new StringSerializer(), new StringSerializer(), PRODUCER_1)) {
      assertThat(producer).isNotNull();
      assertThat(KafkaHelper.getClientId(producer)).isEqualTo(PRODUCER_1);
    }
  }

  @Test
  public void shouldReturnProducerByProducerConfig() {
    try (KafkaProducer<String, String> producer =
        kafkaBundle.createProducer(
            null,
            null,
            ProducerConfig.builder()
                .withClientId("puff-the-magic-producer")
                .addConfig(
                    "key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
                .addConfig(
                    "value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
                .build())) {
      assertThat(producer).isNotNull();
      assertThat(KafkaHelper.getClientId(producer)).isEqualTo("puff-the-magic-producer");
    }
  }

  @Test
  public void testConsumerCanReadMessages() {
    String topic = "testConsumerCanReadMessages";
    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

    kafkaBundle.registerMessageHandler(
        MessageHandlerRegistration // NOSONAR
            .<Long, Long>builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withConsumerConfig(CONSUMER_1)
            .withHandler(record -> results.add(record.value()))
            .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
            .build());

    MessageProducer<Long, Long> producer =
        kafkaBundle.registerProducer(
            ProducerRegistration.<Long, Long>builder()
                .forTopic(topic)
                .withProducerConfig(PRODUCER_1)
                .build());

    // pass in messages
    producer.send(1L, 1L);
    producer.send(2L, 2L);

    await().atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS).until(() -> results.size() == 2);
    assertThat(results).containsExactlyInAnyOrder(1L, 2L);
  }

  @Test
  public void testConsumerCanReadMessagesNamed() {
    String topic = "testConsumerCanReadMessagesNamed";
    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

    kafkaBundle.registerMessageHandler(
        MessageHandlerRegistration // NOSONAR
            .<String, String>builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withConsumerConfig(CONSUMER_2)
            .withHandler(record -> resultsString.add(record.value()))
            .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
            .build());

    MessageProducer<String, String> producer =
        kafkaBundle.registerProducer(
            ProducerRegistration.<String, String>builder()
                .forTopic(topic)
                .checkTopicConfiguration()
                .withProducerConfig(PRODUCER_2)
                .build());

    // pass in messages
    producer.send("1l", "1l");
    producer.send("2l", "2l");

    await()
        .atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS)
        .until(() -> resultsString.size() == 2);
    assertThat(resultsString).containsExactlyInAnyOrder("1l", "2l");
  }

  @Test
  public void defaultConProdShouldHaveStringSerializer() {
    String topic = "defaultConProdShouldHaveStringSerializer";
    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

    kafkaBundle.registerMessageHandler(
        MessageHandlerRegistration // NOSONAR
            .<String, String>builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withDefaultConsumer()
            .withHandler(record -> resultsString.add(record.value()))
            .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
            .build());

    MessageProducer<String, String> producer =
        kafkaBundle.registerProducer(
            ProducerRegistration.<String, String>builder()
                .forTopic(topic)
                .checkTopicConfiguration()
                .withDefaultProducer()
                .build());

    // pass in messages
    producer.send("1l", "1l");
    producer.send("2l", "2l");

    await()
        .atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS)
        .until(() -> resultsString.size() == 2);

    assertThat(resultsString).containsExactlyInAnyOrder("1l", "2l");
  }

  @Test
  public void testKafkaMessages() {
    String topic = "testKafkaMessages";

    List<String> checkMessages = new ArrayList<>();

    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

    kafkaBundle.registerMessageHandler(
        MessageHandlerRegistration // NOSONAR
            .<String, String>builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withDefaultConsumer()
            .withKeyDeserializer(new StringDeserializer())
            .withValueDeserializer(new StringDeserializer())
            .withHandler(record -> resultsString.add(record.value()))
            .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
            .build());

    // pass in messages
    for (int i = 0; i < KafkaBundleConsts.N_MESSAGES; i++) {
      String message = UUID.randomUUID().toString();
      checkMessages.add(message);

      stringStringProducer.send(new ProducerRecord<>(topic, message));
    }

    await()
        .atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS)
        .until(() -> resultsString.size() == checkMessages.size());
    assertThat(resultsString).containsExactlyInAnyOrderElementsOf(checkMessages);
  }

  @Test
  public void producerShouldSendMessagesToKafka() {
    String topic = "producerShouldSendMessagesToKafka";
    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);
    MessageProducer<String, String> producer =
        kafkaBundle.registerProducer(
            ProducerRegistration.<String, String>builder()
                .forTopic(topic)
                .withDefaultProducer()
                .withValueSerializer(new StringSerializer())
                .build());

    assertThat(producer).isNotNull();

    List<String> messages = new ArrayList<>();
    List<String> receivedMessages = new ArrayList<>();

    for (int i = 0; i < KafkaBundleConsts.N_MESSAGES; i++) {
      String message = UUID.randomUUID().toString();
      messages.add(message);
      producer.send("test", message);
    }

    await()
        .atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS)
        .until(
            () -> {
              List<ConsumerRecord<String, String>> consumerRecords =
                  KAFKA
                      .getKafkaTestUtils()
                      .consumeAllRecordsFromTopic(
                          topic, StringDeserializer.class, StringDeserializer.class);
              consumerRecords.forEach(r -> receivedMessages.add(r.value()));
              return receivedMessages.size() == messages.size();
            });

    assertThat(receivedMessages)
        .hasSize(KafkaBundleConsts.N_MESSAGES)
        .containsExactlyInAnyOrderElementsOf(messages);
  }

  @Test
  public void kafkaConsumerReceivesMessages() {

    String topic = "kafkaConsumerReceivesMessages";
    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);
    StringDeserializer deserializer = new StringDeserializer();

    kafkaBundle.registerMessageHandler(
        MessageHandlerRegistration // NOSONAR
            .<String, String>builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withDefaultConsumer()
            .withKeyDeserializer(deserializer)
            .withValueDeserializer(deserializer)
            .withHandler(record -> resultsString.add(record.value()))
            .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
            .build());

    // empty topic before test
    KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(topic);

    List<String> checkMessages = new ArrayList<>();

    // pass in messages
    for (int i = 0; i < KafkaBundleConsts.N_MESSAGES; i++) {
      String message = UUID.randomUUID().toString();
      checkMessages.add(message);

      stringStringProducer.send(new ProducerRecord<>(topic, message));
    }

    await()
        .atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS)
        .until(() -> resultsString.size() == checkMessages.size());
    assertThat(resultsString).containsExactlyElementsOf(checkMessages);
  }

  @Test
  public void kafkaConsumerReceivesMessagesAsyncCommit() {
    String topic = "kafkaConsumerReceivesMessagesAsyncCommit";
    StringDeserializer deserializer = new StringDeserializer();
    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

    // register adhoc implementations
    assertThat(kafkaBundle).isNotNull();

    kafkaBundle.registerMessageHandler(
        MessageHandlerRegistration // NOSONAR
            .<String, String>builder()
            .withListenerConfig(LISTENER_CONFIG_ASYNC)
            .forTopic(topic)
            .withConsumerConfig(
                ConsumerConfig.builder()
                    .withGroup(UUID.randomUUID().toString())
                    .withClientId(CLIENT_ID_ASYNC)
                    .build())
            .withKeyDeserializer(deserializer)
            .withValueDeserializer(deserializer)
            .withHandler(
                new CallbackMessageHandler<String, String>() {
                  @Override
                  public void handleCommitCallback(
                      Map<TopicPartition, OffsetAndMetadata> offsets, Exception exception) {
                    callbackCount++;
                  }

                  @Override
                  public void handle(ConsumerRecord<String, String> record) {
                    resultsString.add(record.value());
                  }
                })
            .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
            .build());

    List<String> checkMessages = new ArrayList<>();
    // pass in messages
    for (int i = 0; i < KafkaBundleConsts.N_MESSAGES; i++) {
      String message = UUID.randomUUID().toString();
      checkMessages.add(message);
      stringStringProducer.send(new ProducerRecord<>(topic, message));
    }

    await()
        .atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS)
        .untilAsserted(
            () -> {
              assertThat(resultsString).containsExactlyInAnyOrderElementsOf(checkMessages);
              assertThat(callbackCount).isGreaterThan(0);
            });
  }

  @Test
  public void multiTest() {

    final String TOPIC_CREATE = "create";
    final String TOPIC_DELETE = "delete";

    KAFKA.getKafkaTestUtils().createTopic(TOPIC_CREATE, 1, (short) 1);
    KAFKA.getKafkaTestUtils().createTopic(TOPIC_DELETE, 1, (short) 1);

    kafkaBundle.registerMessageHandler(
        MessageHandlerRegistration // NOSONAR
            .<Long, String>builder()
            .withDefaultListenerConfig()
            .forTopic(TOPIC_CREATE)
            .withDefaultConsumer()
            .withKeyDeserializer(new LongDeserializer())
            .withHandler(record -> resultsString.add(record.value()))
            .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
            .build());

    kafkaBundle.registerMessageHandler(
        MessageHandlerRegistration // NOSONAR
            .<String, String>builder()
            .withDefaultListenerConfig()
            .forTopic(TOPIC_DELETE)
            .withDefaultConsumer()
            .withHandler(record -> resultsString.add(record.value()))
            .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
            .build());

    MessageProducer<Long, String> createProducer =
        kafkaBundle.registerProducer(
            ProducerRegistration.<Long, String>builder()
                .forTopic(TOPIC_CREATE)
                .withProducerConfig(new ProducerConfig())
                .withKeySerializer(new LongSerializer())
                .build());

    MessageProducer<String, String> deleteProducer =
        kafkaBundle.registerProducer(
            ProducerRegistration.<String, String>builder()
                .forTopic(TOPIC_DELETE)
                .withDefaultProducer()
                .build());

    createProducer.send(1L, "test1");
    deleteProducer.send("key", "test2");

    await()
        .atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS)
        .until(() -> resultsString.size() == 2);

    assertThat(resultsString).containsExactlyInAnyOrder("test1", "test2");
  }

  @Test
  public void testJsonSerializer() {
    String topic = "testJsonSerializer";
    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

    kafkaBundle.registerMessageHandler(
        MessageHandlerRegistration.<String, SimpleEntity>builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withDefaultConsumer()
            .withValueDeserializer(
                new KafkaJsonDeserializer<>(new ObjectMapper(), SimpleEntity.class))
            .withHandler(x -> resultsString.add(x.value().getName()))
            .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
            .build());

    MessageProducer<String, SimpleEntity> prod =
        kafkaBundle.registerProducer(
            ProducerRegistration.<String, SimpleEntity>builder()
                .forTopic(topic)
                .withDefaultProducer()
                .withValueSerializer(new KafkaJsonSerializer<>(new ObjectMapper()))
                .build());

    SimpleEntity a = new SimpleEntity();
    a.setName("a");

    SimpleEntity b = new SimpleEntity();
    b.setName("b");

    prod.send("Test", a);
    prod.send("Test", b);

    await()
        .atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS)
        .until(() -> resultsString.size() == 2);

    assertThat(resultsString).containsExactlyInAnyOrder("a", "b");
  }

  @Test
  public void testValueWrappedNoSerializationErrorDeserializer() {
    String topic = "testWrappedNoSerializationErrorDeserializer";
    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

    kafkaBundle.registerMessageHandler(
        MessageHandlerRegistration.<String, SimpleEntity>builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withDefaultConsumer()
            .withValueDeserializer(
                new WrappedNoSerializationErrorDeserializer<>(
                    new KafkaJsonDeserializer<>(new ObjectMapper(), SimpleEntity.class)))
            .withHandler(
                x -> {
                  if (x.value() != null) {
                    resultsString.add(x.value().getName());
                  }
                })
            .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
            .build());

    try (KafkaProducer<String, String> producer =
        KAFKA
            .getKafkaTestUtils()
            .getKafkaProducer(StringSerializer.class, StringSerializer.class)) {
      producer.send(
          new ProducerRecord<>(
              topic, "Test", "{ \"name\":\"Heinz\", \"lastname\":\"Mustermann\"}"));
      producer.send(new ProducerRecord<>(topic, "Test", "invalid json value"));
      producer.send(
          new ProducerRecord<>(
              topic, "Test", "{ \"name\":\"Heidi\", \"lastname\":\"Musterfrau\"}"));
    }

    await()
        .atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS)
        .until(() -> resultsString.size() == 2);

    assertThat(resultsString).containsExactlyInAnyOrder("Heinz", "Heidi");
  }

  @Test
  public void testKeyWrappedNoSerializationErrorDeserializer() {
    String topic = "testKeyWrappedNoSerializationErrorDeserializer";
    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

    kafkaBundle.registerMessageHandler(
        MessageHandlerRegistration.<SimpleEntity, String>builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withDefaultConsumer()
            .withKeyDeserializer(
                new WrappedNoSerializationErrorDeserializer<>(
                    new KafkaJsonDeserializer<>(new ObjectMapper(), SimpleEntity.class)))
            .withHandler(
                x -> {
                  if (x.key() != null && x.value() != null) {
                    resultsString.add(x.value());
                  }
                })
            .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
            .build());

    try (KafkaProducer<String, String> producer =
        KAFKA
            .getKafkaTestUtils()
            .getKafkaProducer(StringSerializer.class, StringSerializer.class)) {

      producer.send(
          new ProducerRecord<>(topic, "{ \"name\":\"Heinz\", \"lastname\":\"Mustermann\"}", "a"));
      producer.send(new ProducerRecord<>(topic, "invalid json key", "b"));
      producer.send(
          new ProducerRecord<>(topic, "{ \"name\":\"Heidi\", \"lastname\":\"Musterfrau\"}", "c"));

      await()
          .atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS)
          .until(() -> resultsString.size() == 2);

      assertThat(resultsString).containsExactlyInAnyOrder("a", "c");
    }
  }

  @Test
  public void shouldCreateSeveralInstancesOfConsumer() {
    String topic = "shouldCreateSeveralInstancesOfConsumer";
    // when
    List<MessageListener<SimpleEntity, String>> listener =
        kafkaBundle.registerMessageHandler(
            MessageHandlerRegistration.<SimpleEntity, String>builder()
                .withListenerConfig(ListenerConfig.builder().build(2))
                .forTopic(topic)
                .withConsumerConfig(ConsumerConfig.builder().withClientId("myclient").build())
                .withKeyDeserializer(
                    new WrappedNoSerializationErrorDeserializer<>(
                        new KafkaJsonDeserializer<>(new ObjectMapper(), SimpleEntity.class)))
                .withHandler(
                    x -> {
                      if (x.key() != null && x.value() != null) {
                        resultsString.add(x.value());
                      }
                    })
                .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
                .build());

    // then
    Set<String> clientIds = new HashSet<>();
    clientIds.add(KafkaHelper.getClientId(listener.get(0).getConsumer()));
    clientIds.add(KafkaHelper.getClientId(listener.get(1).getConsumer()));
    assertThat(clientIds).containsExactlyInAnyOrder("myclient-0", "myclient-1");
  }

  @Test
  public void shouldSetProducerNameCorrectlyWithProducerConfig() {
    try (KafkaProducer<String, String> p1 =
        kafkaBundle.createProducer(
            new StringSerializer(),
            new StringSerializer(),
            ProducerConfig.builder().withClientId("p1").build())) {
      assertThat(KafkaHelper.getClientId(p1)).isEqualTo("p1");
    }
  }

  @Test
  public void shouldSetProducerNameCorrectlyWithProducerConfigFromYaml() {
    try (KafkaProducer<String, String> p1 =
        kafkaBundle.createProducer(new StringSerializer(), new StringSerializer(), PRODUCER_1)) {
      assertThat(KafkaHelper.getClientId(p1)).isEqualTo(PRODUCER_1);
    }
  }

  @Test
  public void shouldSetProducerNameCorrectlyWithProducerConfigFromYamlWithExplicitClientId() {
    try (KafkaProducer<String, String> p1 =
        kafkaBundle.createProducer(new StringSerializer(), new StringSerializer(), PRODUCER_2)) {
      assertThat(KafkaHelper.getClientId(p1)).isEqualTo("p2");
    }
  }

  @Test
  public void shouldSetConsumerNameCorrectlyWithConsumerConfig() {
    try (KafkaConsumer<String, String> c1 =
        kafkaBundle.createConsumer(
            new StringDeserializer(),
            new StringDeserializer(),
            ConsumerConfig.builder().withClientId("c1").build(),
            1)) {

      assertThat(KafkaHelper.getClientId(c1)).isEqualTo("c1-1");
    }
  }

  @Test
  public void shouldSetConsumerNameCorrectlyWithConsumerConfigFromYaml() {
    try (KafkaConsumer<String, String> c1 =
        kafkaBundle.createConsumer(
            new StringDeserializer(), new StringDeserializer(), CONSUMER_1)) {

      assertThat(KafkaHelper.getClientId(c1)).isEqualTo(CONSUMER_1 + "-0");
    }
  }

  @Test
  public void shouldSetConsumerNameCorrectlyWithConsumerConfigFromYamlWithExplicitClientId() {
    try (KafkaConsumer<String, String> c1 =
        kafkaBundle.createConsumer(
            new StringDeserializer(), new StringDeserializer(), CONSUMER_2)) {

      assertThat(KafkaHelper.getClientId(c1)).isEqualTo("c2-0");
    }
  }

  public class ProcessingRecordException extends RuntimeException {

    ProcessingRecordException(String message) {
      super(message);
    }
  }
}
