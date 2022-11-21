package org.sdase.commons.server.kafka;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.kafka.builder.MessageListenerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.config.ConsumerConfig;
import org.sdase.commons.server.kafka.config.ListenerConfig;
import org.sdase.commons.server.kafka.config.ProducerConfig;
import org.sdase.commons.server.kafka.consumer.ErrorHandler;
import org.sdase.commons.server.kafka.consumer.IgnoreAndProceedErrorHandler;
import org.sdase.commons.server.kafka.consumer.KafkaHelper;
import org.sdase.commons.server.kafka.consumer.MessageHandler;
import org.sdase.commons.server.kafka.consumer.MessageListener;
import org.sdase.commons.server.kafka.consumer.strategies.MessageListenerStrategy;
import org.sdase.commons.server.kafka.consumer.strategies.autocommit.AutocommitMLS;
import org.sdase.commons.server.kafka.consumer.strategies.synccommit.SyncCommitMLS;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import org.sdase.commons.server.kafka.exception.ConfigurationException;
import org.sdase.commons.server.kafka.producer.MessageProducer;
import org.sdase.commons.server.kafka.serializers.KafkaJsonDeserializer;
import org.sdase.commons.server.kafka.serializers.KafkaJsonSerializer;
import org.sdase.commons.server.kafka.serializers.SimpleEntity;
import org.sdase.commons.server.kafka.serializers.WrappedNoSerializationErrorDeserializer;

class KafkaBundleWithConfigIT {

  @RegisterExtension
  @Order(0)
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

  @RegisterExtension
  @Order(1)
  private static final DropwizardAppExtension<KafkaTestConfiguration> DROPWIZARD_APP_EXTENSION =
      new DropwizardAppExtension<>(
          KafkaTestApplication.class,
          resourceFilePath("test-config-default.yml"),
          config("kafka.brokers", KAFKA::getKafkaConnectString),

          // performance improvements in the tests
          config("kafka.config.heartbeat\\.interval\\.ms", "250"),
          config("kafka.adminConfig.adminClientRequestTimeoutMs", "30000"));

  private final List<Long> results = Collections.synchronizedList(new ArrayList<>());
  private final List<String> resultsString = Collections.synchronizedList(new ArrayList<>());

  private KafkaBundle<KafkaTestConfiguration> kafkaBundle;
  private int callbackCount = 0;

  private static KafkaProducer<String, String> stringStringProducer;

  @BeforeAll
  static void beforeAll() {
    stringStringProducer =
        KAFKA.getKafkaTestUtils().getKafkaProducer(StringSerializer.class, StringSerializer.class);
  }

  @AfterAll
  static void afterAll() {
    stringStringProducer.close();
  }

  @BeforeEach
  void before() {
    KafkaTestApplication app = DROPWIZARD_APP_EXTENSION.getApplication();
    kafkaBundle = app.kafkaBundle();
    results.clear();
    resultsString.clear();
  }

  @Test
  void healthCheckShouldBeAdded() {
    KafkaTestApplication app = DROPWIZARD_APP_EXTENSION.getApplication();
    assertThat(app.healthCheckRegistry().getHealthCheck("kafkaConnection")).isNotNull();
  }

  @Test
  void allTopicsDescriptionsGenerated() {
    final String testTopic1 = "topicId1";
    assertThat(kafkaBundle.getTopicConfiguration(testTopic1)).isNotNull();
    assertThat(kafkaBundle.getTopicConfiguration(testTopic1).getReplicationFactor().count())
        .isEqualTo(2);
    assertThat(kafkaBundle.getTopicConfiguration(testTopic1).getPartitions().count()).isEqualTo(2);
    assertThat(kafkaBundle.getTopicConfiguration(testTopic1).getProps()).hasSize(2);
    assertThat(kafkaBundle.getTopicConfiguration("topicId2")).isNotNull();
  }

  @Test
  void createProducerWithTopic() {
    MessageProducer<String, String> topicName2 =
        kafkaBundle.registerProducer(
            ProducerRegistration.builder()
                .forTopic(kafkaBundle.getTopicConfiguration("topicId2"))
                .withDefaultProducer()
                .withKeySerializer(new StringSerializer())
                .withValueSerializer(new StringSerializer())
                .build());
    assertThat(topicName2).isNotNull();
  }

  @Test
  void autocommitStrategyShouldCommit() {
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
        MessageListenerRegistration.builder()
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

  @Test
  void shouldProduceConfigExceptionWhenConsumerConfigNotExists() {
    assertThrows(
        ConfigurationException.class,
        () -> {
          try (KafkaConsumer<String, String> consumer =
              kafkaBundle.createConsumer(
                  new StringDeserializer(),
                  new StringDeserializer(),
                  "notExistingConsumerConfig")) {
            // empty
          }
        });
  }

  @Test
  void shouldReturnConsumerByConsumerConfigName() {
    try (KafkaConsumer<String, String> consumer =
        kafkaBundle.createConsumer(
            new StringDeserializer(), new StringDeserializer(), CONSUMER_1)) {
      assertThat(consumer).isNotNull();
      assertThat(KafkaHelper.getClientId(consumer)).isEqualTo(CONSUMER_1 + "-0");
    }
  }

  @Test
  void shouldReturnConsumerByConsumerConfig() {
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

  @Test
  void shouldProduceConfigExceptionWhenProducerConfigNotExists() {
    assertThrows(
        ConfigurationException.class,
        () -> {
          try (KafkaProducer<String, String> producer =
              kafkaBundle.createProducer(
                  new StringSerializer(), new StringSerializer(), "notExistingProducerConfig")) {
            // empty
          }
        });
  }

  @Test
  void shouldReturnProducerByProducerConfigName() {
    try (KafkaProducer<String, String> producer =
        kafkaBundle.createProducer(new StringSerializer(), new StringSerializer(), PRODUCER_1)) {
      assertThat(producer).isNotNull();
      assertThat(KafkaHelper.getClientId(producer)).isEqualTo(PRODUCER_1);
    }
  }

  @Test
  void shouldReturnProducerByProducerConfig() {
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
  void testConsumerCanReadMessages() {
    String topic = "testConsumerCanReadMessages";
    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

    kafkaBundle.createMessageListener(
        MessageListenerRegistration.builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withConsumerConfig(CONSUMER_1)
            .withListenerStrategy(
                new SyncCommitMLS<Long, Long>(
                    record -> results.add(record.value()), new IgnoreAndProceedErrorHandler<>()))
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
  void testConsumerCanReadMessagesNamed() {
    String topic = "testConsumerCanReadMessagesNamed";
    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

    kafkaBundle.createMessageListener(
        MessageListenerRegistration.builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withConsumerConfig(CONSUMER_2)
            .withListenerStrategy(
                new SyncCommitMLS<String, String>(
                    record -> resultsString.add(record.value()),
                    new IgnoreAndProceedErrorHandler<>()))
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
  void defaultConProdShouldHaveStringSerializer() {
    String topic = "defaultConProdShouldHaveStringSerializer";
    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

    kafkaBundle.createMessageListener(
        MessageListenerRegistration.builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withDefaultConsumer()
            .withListenerStrategy(
                new SyncCommitMLS<String, String>(
                    record -> resultsString.add(record.value()),
                    new IgnoreAndProceedErrorHandler<>()))
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
  void testKafkaMessages() {
    String topic = "testKafkaMessages";

    List<String> checkMessages = new ArrayList<>();

    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

    kafkaBundle.createMessageListener(
        MessageListenerRegistration.builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withDefaultConsumer()
            .withKeyDeserializer(new StringDeserializer())
            .withValueDeserializer(new StringDeserializer())
            .withListenerStrategy(
                new SyncCommitMLS<>(
                    record -> resultsString.add(record.value()),
                    new IgnoreAndProceedErrorHandler<>()))
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
  void producerShouldSendMessagesToKafka() {
    String topic = "producerShouldSendMessagesToKafka";
    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);
    MessageProducer<String, String> producer =
        kafkaBundle.registerProducer(
            ProducerRegistration.builder()
                .forTopic(topic)
                .withDefaultProducer()
                .withKeySerializer(new StringSerializer())
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
  void kafkaConsumerReceivesMessages() {

    String topic = "kafkaConsumerReceivesMessages";
    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);
    StringDeserializer deserializer = new StringDeserializer();

    kafkaBundle.createMessageListener(
        MessageListenerRegistration.builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withDefaultConsumer()
            .withKeyDeserializer(deserializer)
            .withValueDeserializer(deserializer)
            .withListenerStrategy(
                new SyncCommitMLS<>(
                    record -> resultsString.add(record.value()),
                    new IgnoreAndProceedErrorHandler<>()))
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
        .until(() -> resultsString.size() == checkMessages.size());
    assertThat(resultsString).containsExactlyElementsOf(checkMessages);
  }

  @Test
  void kafkaConsumerReceivesMessagesAsyncCommit() {
    String topic = "kafkaConsumerReceivesMessagesAsyncCommit";
    StringDeserializer deserializer = new StringDeserializer();
    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

    // register adhoc implementations
    assertThat(kafkaBundle).isNotNull();

    kafkaBundle.createMessageListener(
        MessageListenerRegistration.builder()
            .withListenerConfig(LISTENER_CONFIG_ASYNC)
            .forTopic(topic)
            .withConsumerConfig(
                ConsumerConfig.builder()
                    .withGroup(UUID.randomUUID().toString())
                    .withClientId(CLIENT_ID_ASYNC)
                    .build())
            .withKeyDeserializer(deserializer)
            .withValueDeserializer(deserializer)
            .withListenerStrategy(
                new MessageListenerStrategy<String, String>() {
                  @Override
                  public void processRecords(
                      ConsumerRecords<String, String> records,
                      KafkaConsumer<String, String> consumer) {
                    records.forEach(r -> resultsString.add(r.value()));
                    consumer.commitSync();
                    callbackCount++;
                  }

                  @Override
                  public void commitOnClose(KafkaConsumer<String, String> consumer) {}

                  @Override
                  public void verifyConsumerConfig(Map<String, String> config) {}
                })
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
              assertThat(callbackCount).isPositive();
            });
  }

  @Test
  void multiTest() {

    final String TOPIC_CREATE = "create";
    final String TOPIC_DELETE = "delete";

    KAFKA.getKafkaTestUtils().createTopic(TOPIC_CREATE, 1, (short) 1);
    KAFKA.getKafkaTestUtils().createTopic(TOPIC_DELETE, 1, (short) 1);

    kafkaBundle.createMessageListener(
        MessageListenerRegistration.builder()
            .withDefaultListenerConfig()
            .forTopic(TOPIC_CREATE)
            .withDefaultConsumer()
            .withKeyDeserializer(new LongDeserializer())
            .withListenerStrategy(
                new SyncCommitMLS<Long, String>(
                    record -> resultsString.add(record.value()),
                    new IgnoreAndProceedErrorHandler<>()))
            .build());

    kafkaBundle.createMessageListener(
        MessageListenerRegistration.builder()
            .withDefaultListenerConfig()
            .forTopic(TOPIC_DELETE)
            .withDefaultConsumer()
            .withListenerStrategy(
                new SyncCommitMLS<String, String>(
                    record -> resultsString.add(record.value()),
                    new IgnoreAndProceedErrorHandler<>()))
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
  void testJsonSerializer() {
    String topic = "testJsonSerializer";
    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

    kafkaBundle.createMessageListener(
        MessageListenerRegistration.builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withDefaultConsumer()
            .withValueDeserializer(
                new KafkaJsonDeserializer<>(new ObjectMapper(), SimpleEntity.class))
            .withListenerStrategy(
                new SyncCommitMLS<>(
                    x -> resultsString.add(x.value().getName()),
                    new IgnoreAndProceedErrorHandler<>()))
            .build());

    MessageProducer<Object, SimpleEntity> prod =
        kafkaBundle.registerProducer(
            ProducerRegistration.builder()
                .forTopic(topic)
                .withDefaultProducer()
                .withValueSerializer(new KafkaJsonSerializer<SimpleEntity>(new ObjectMapper()))
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
  void testValueWrappedNoSerializationErrorDeserializer() {
    String topic = "testWrappedNoSerializationErrorDeserializer";
    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

    kafkaBundle.createMessageListener(
        MessageListenerRegistration.builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withDefaultConsumer()
            .withValueDeserializer(
                new WrappedNoSerializationErrorDeserializer<>(
                    new KafkaJsonDeserializer<>(new ObjectMapper(), SimpleEntity.class)))
            .withListenerStrategy(
                new SyncCommitMLS<>(
                    x -> {
                      if (x.value() != null) {
                        resultsString.add(x.value().getName());
                      }
                    },
                    new IgnoreAndProceedErrorHandler<>()))
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
  void testKeyWrappedNoSerializationErrorDeserializer() {
    String topic = "testKeyWrappedNoSerializationErrorDeserializer";
    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

    kafkaBundle.createMessageListener(
        MessageListenerRegistration.builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withDefaultConsumer()
            .withKeyDeserializer(
                new WrappedNoSerializationErrorDeserializer<>(
                    new KafkaJsonDeserializer<>(new ObjectMapper(), SimpleEntity.class)))
            .withListenerStrategy(
                new SyncCommitMLS<SimpleEntity, String>(
                    x -> {
                      if (x.key() != null && x.value() != null) {
                        resultsString.add(x.value());
                      }
                    },
                    new IgnoreAndProceedErrorHandler<>()))
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
  void shouldCreateSeveralInstancesOfConsumer() {
    String topic = "shouldCreateSeveralInstancesOfConsumer";
    // when
    List<MessageListener<SimpleEntity, String>> listener =
        kafkaBundle.createMessageListener(
            MessageListenerRegistration.builder()
                .withListenerConfig(ListenerConfig.builder().build(2))
                .forTopic(topic)
                .withConsumerConfig(ConsumerConfig.builder().withClientId("myclient").build())
                .withKeyDeserializer(
                    new WrappedNoSerializationErrorDeserializer<>(
                        new KafkaJsonDeserializer<>(new ObjectMapper(), SimpleEntity.class)))
                .withListenerStrategy(
                    new SyncCommitMLS<SimpleEntity, String>(
                        x -> {
                          if (x.key() != null && x.value() != null) {
                            resultsString.add(x.value());
                          }
                        },
                        new IgnoreAndProceedErrorHandler<>()))
                .build());

    // then
    Set<String> clientIds = new HashSet<>();
    clientIds.add(KafkaHelper.getClientId(listener.get(0).getConsumer()));
    clientIds.add(KafkaHelper.getClientId(listener.get(1).getConsumer()));
    assertThat(clientIds).containsExactlyInAnyOrder("myclient-0", "myclient-1");
  }

  @Test
  void shouldSetProducerNameCorrectlyWithProducerConfig() {
    try (KafkaProducer<String, String> p1 =
        kafkaBundle.createProducer(
            new StringSerializer(),
            new StringSerializer(),
            ProducerConfig.builder().withClientId("p1").build())) {
      assertThat(KafkaHelper.getClientId(p1)).isEqualTo("p1");
    }
  }

  @Test
  void shouldSetProducerNameCorrectlyWithProducerConfigFromYaml() {
    try (KafkaProducer<String, String> p1 =
        kafkaBundle.createProducer(new StringSerializer(), new StringSerializer(), PRODUCER_1)) {
      assertThat(KafkaHelper.getClientId(p1)).isEqualTo(PRODUCER_1);
    }
  }

  @Test
  void shouldSetProducerNameCorrectlyWithProducerConfigFromYamlWithExplicitClientId() {
    try (KafkaProducer<String, String> p1 =
        kafkaBundle.createProducer(new StringSerializer(), new StringSerializer(), PRODUCER_2)) {
      assertThat(KafkaHelper.getClientId(p1)).isEqualTo("p2");
    }
  }

  @Test
  void shouldSetConsumerNameCorrectlyWithConsumerConfig() {
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
  void shouldSetConsumerNameCorrectlyWithConsumerConfigFromYamlWithExplicitClientId() {
    try (KafkaConsumer<String, String> c1 =
        kafkaBundle.createConsumer(
            new StringDeserializer(), new StringDeserializer(), CONSUMER_2)) {

      assertThat(KafkaHelper.getClientId(c1)).isEqualTo("c2-0");
    }
  }

  public static class ProcessingRecordException extends RuntimeException {

    ProcessingRecordException(String message) {
      super(message);
    }
  }
}
