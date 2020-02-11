package org.sdase.commons.server.kafka;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.startsWith;

import com.salesforce.kafka.test.KafkaBroker;
import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sdase.commons.server.kafka.builder.MessageListenerRegistration;
import org.sdase.commons.server.kafka.config.ConsumerConfig;
import org.sdase.commons.server.kafka.config.TopicConfig;
import org.sdase.commons.server.kafka.consumer.ErrorHandler;
import org.sdase.commons.server.kafka.consumer.MessageHandler;
import org.sdase.commons.server.kafka.consumer.MessageListener;
import org.sdase.commons.server.kafka.consumer.strategies.deadletter.DeadLetterMLS;
import org.sdase.commons.server.kafka.consumer.strategies.deadletter.KafkaClientManager;
import org.sdase.commons.server.kafka.consumer.strategies.deadletter.helper.NoSerializationErrorDeserializer;
import org.sdase.commons.server.kafka.consumer.strategies.retryprocessingerror.ProcessingErrorRetryException;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.sdase.commons.server.testing.LazyRule;

@SuppressWarnings("unchecked")
public class KafkaConsumerWithDeadLetterIT extends KafkaBundleConsts {

  private static final SharedKafkaTestResource KAFKA =
      new SharedKafkaTestResource()
          .withBrokerProperty("auto.create.topics.enable", "false")
          // we only need one consumer offsets partition
          .withBrokerProperty("offsets.topic.num.partitions", "1")
          // we don't need to wait that a consumer group rebalances since we always start with a
          // fresh kafka instance
          .withBrokerProperty("group.initial.rebalance.delay.ms", "0");

  private static int noTestCases = 20;

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
                        IntStream.range(1, noTestCases + 1)
                            .forEach(no -> addTopicConfigForTestcase(kafka, no));
                        // for testcase id 99, only main topic is configured
                        kafka
                            .getTopics()
                            .put(
                                calcTopicNames(99)[0],
                                TopicConfig.builder().name(calcTopicNames(99)[0]).build());
                      })
                  .build());

  @ClassRule
  public static final TestRule CHAIN = RuleChain.outerRule(KAFKA).around(DROPWIZARD_APP_RULE);

  private KafkaBundle<KafkaTestConfiguration> bundle =
      ((KafkaTestApplication) DROPWIZARD_APP_RULE.getRule().getApplication()).kafkaBundle();
  private Deserializer keyDeserializer = new StringDeserializer();
  private Deserializer valueDeserializer = new IntegerDeserializer();

  private static void addTopicConfigForTestcase(KafkaConfiguration kafka, int testcaseNo) {
    String[] topicNames = calcTopicNames(testcaseNo);

    kafka
        .getTopics()
        .put(topicNames[0] + "-retry", TopicConfig.builder().name(topicNames[1]).build());
    kafka
        .getTopics()
        .put(topicNames[0] + "-deadLetter", TopicConfig.builder().name(topicNames[2]).build());
    kafka.getTopics().put(topicNames[0], TopicConfig.builder().name(topicNames[0]).build());
  }

  private static Object deserialize(byte[] obj) {
    try {
      ByteArrayInputStream bis = new ByteArrayInputStream(obj);
      ObjectInput in = new ObjectInputStream(bis);
      return in.readObject();
    } catch (ClassNotFoundException e) {
      return null;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  private static String[] calcTopicNames(int testcaseNo) {
    String main = "mainTopic" + testcaseNo;
    String retry = main + ".retry";
    String deadLetter = main + ".deadLetter";
    return new String[] {main, retry, deadLetter};
  }

  private ConsumerConfig getConsumerConfig() {
    return ConsumerConfig.builder()
        .withGroup("test")
        .addConfig("enable.auto.commit", "false")
        .addConfig("max.poll.records", "5")
        .build();
  }

  private KafkaProducer<String, Integer> getKafkaProducer() {
    return KAFKA
        .getKafkaTestUtils()
        .getKafkaProducer(StringSerializer.class, IntegerSerializer.class);
  }

  private void createTopics(String[] topicNames) {
    KAFKA.getKafkaTestUtils().createTopic(topicNames[0], 1, (short) 1);
    KAFKA.getKafkaTestUtils().createTopic(topicNames[1], 1, (short) 1);
    KAFKA.getKafkaTestUtils().createTopic(topicNames[2], 1, (short) 1);
  }

  @Test
  public void shouldCreateTopicsAndConsumerIfOnlyMainTopicIsInConfig() {
    String[] topics = calcTopicNames(99);

    // create only main topic
    KAFKA.getKafkaTestUtils().createTopic(topics[0], 1, (short) 1);

    List<MessageListener> listener =
        bundle.createMessageListener(
            MessageListenerRegistration.<String, Integer>builder()
                .withDefaultListenerConfig()
                .forTopic(topics[0])
                .withConsumerConfig(getConsumerConfig())
                .withValueDeserializer(new NoSerializationErrorDeserializer<>(valueDeserializer))
                .withKeyDeserializer(new NoSerializationErrorDeserializer<>(keyDeserializer))
                .withListenerStrategy(
                    new DeadLetterMLS(
                        DROPWIZARD_APP_RULE.getRule().getEnvironment(),
                        (record) -> {},
                        new KafkaClientManager(bundle, topics[0]),
                        4,
                        1000))
                .build());

    KAFKA.getKafkaTestUtils().produceRecords(2, topics[2], 0);

    await()
        .atMost(15, SECONDS)
        .pollInterval(1, SECONDS)
        .untilAsserted(
            () -> {
              try (AdminClient admin = KAFKA.getKafkaTestUtils().getAdminClient()) {
                Set<String> existingTopics = admin.listTopics().names().get();
                assertThat("retry topic is created", existingTopics.contains(topics[1]));
                assertThat("deadLetter topic is created", existingTopics.contains(topics[2]));
              }
              assertThat(
                  "dead letter topic contains 2 messages",
                  KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(topics[2]).size(),
                  equalTo(2));
            });

    listener.forEach(MessageListener::stopConsumer);
  }

  @Test
  public void deadLetterShouldBeSentToDeadLetterTopic() {
    String[] topics = calcTopicNames(1);
    createTopics(topics);

    AtomicInteger processingError = new AtomicInteger(0);
    List<Integer> testResults = Collections.synchronizedList(new ArrayList<>());

    MessageHandler<String, Integer> handler =
        record -> {
          Integer value = record.value();
          if (value == 2) {
            processingError.incrementAndGet();
            throw new ProcessingErrorRetryException("processing error of record: " + record.key());
          }
          testResults.add(value);
        };

    ErrorHandler<String, Integer> errorHandler =
        (record, e, consumer) -> {
          throw new ProcessingErrorRetryException("seems that exception cannot be handled");
        };

    List<MessageListener> listener =
        bundle.createMessageListener(
            MessageListenerRegistration.<String, Integer>builder()
                .withDefaultListenerConfig()
                .forTopic(topics[0])
                .withConsumerConfig(getConsumerConfig())
                .withValueDeserializer(new NoSerializationErrorDeserializer<>(valueDeserializer))
                .withKeyDeserializer(new NoSerializationErrorDeserializer<>(keyDeserializer))
                .withListenerStrategy(
                    new DeadLetterMLS(
                        DROPWIZARD_APP_RULE.getRule().getEnvironment(),
                        handler,
                        errorHandler,
                        new KafkaClientManager(bundle, topics[0]),
                        4,
                        1000))
                .build());

    try (KafkaProducer<String, Integer> producer = getKafkaProducer()) {
      IntStream.range(1, 4)
          .forEach(
              e -> producer.send(new ProducerRecord<>(topics[0], UUID.randomUUID().toString(), e)));
    }

    await()
        .atMost(15, SECONDS)
        .pollInterval(1, SECONDS)
        .untilAsserted(
            () -> {
              assertThat("Test result should contain messages", testResults.size(), equalTo(2));
              assertThat(
                  "One message with 4 retries processing errors",
                  processingError.get(),
                  equalTo(5));
              assertThat(testResults, containsInAnyOrder(1, 3));
              assertThat(
                  "There are records in the retry topic",
                  KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(topics[1]).size(),
                  equalTo(4));
              assertThat(
                  "There are two records in the dead letter topic",
                  KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(topics[2]).size(),
                  equalTo(1));
            });

    listener.forEach(MessageListener::stopConsumer);
  }

  @Test
  public void testNullInputOfRecordGoodCase() {
    String[] topics = calcTopicNames(9);
    createTopics(topics);

    List<Object> testResults = Collections.synchronizedList(new ArrayList<>());

    List<MessageListener> listener = initDefaultTestMessageListener(testResults, topics[0]);

    // null key, ok value
    try (KafkaProducer<String, Integer> producer = getKafkaProducer()) {
      producer.send(new ProducerRecord<>(topics[0], null, 1));
    }

    await()
        .atMost(15, SECONDS)
        .pollInterval(1, SECONDS)
        .untilAsserted(
            () -> {
              assertThat("Test result should contain messages", testResults.size(), equalTo(1));
              assertThat(testResults, containsInAnyOrder(1));
              assertThat(
                  "There must be no messages in retry topic",
                  KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(topics[1]).size(),
                  equalTo(0));
              assertThat(
                  "There must be no messages in dead letter topic",
                  KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(topics[2]).size(),
                  equalTo(0));
            });

    listener.forEach(MessageListener::stopConsumer);
  }

  @Test
  public void testNullInputOfRecordBadCase() {
    String[] topics = calcTopicNames(10);
    createTopics(topics);

    List<Object> testResults = Collections.synchronizedList(new ArrayList<>());

    List<MessageListener> listener = initDefaultTestMessageListener(testResults, topics[0]);

    // null key, faulty value
    try (KafkaProducer<byte[], String> producer =
        KAFKA
            .getKafkaTestUtils()
            .getKafkaProducer(ByteArraySerializer.class, StringSerializer.class)) {
      producer.send(new ProducerRecord<>(topics[0], null, "test123"));
    }

    await()
        .atMost(15, SECONDS)
        .pollInterval(1, SECONDS)
        .untilAsserted(
            () -> {
              assertThat("Test result should contain messages", testResults.size(), equalTo(0));
              assertThat(
                  "There must be no messages in retry topic",
                  KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(topics[1]).size(),
                  equalTo(4));
              assertThat(
                  "There must be no messages in dead letter topic",
                  KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(topics[2]).size(),
                  equalTo(1));
            });

    listener.forEach(MessageListener::stopConsumer);
  }

  private List<MessageListener> initDefaultTestMessageListener(
      List<Object> testResults, String topic) {
    MessageHandler<String, Integer> handler = record -> testResults.add(record.value());

    return bundle.createMessageListener(
        MessageListenerRegistration.<String, Integer>builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withConsumerConfig(getConsumerConfig())
            .withValueDeserializer(new NoSerializationErrorDeserializer<>(valueDeserializer))
            .withKeyDeserializer(new NoSerializationErrorDeserializer<>(keyDeserializer))
            .withListenerStrategy(
                new DeadLetterMLS(
                    DROPWIZARD_APP_RULE.getRule().getEnvironment(),
                    handler,
                    new KafkaClientManager(bundle, topic),
                    4,
                    1000))
            .build());
  }

  @Test
  public void deadLetterMessagesShouldContainHeaders() {
    String[] topics = calcTopicNames(2);
    createTopics(topics);
    List<Integer> testResults = Collections.synchronizedList(new ArrayList<>());

    MessageHandler<String, Integer> handler =
        record -> {
          // no infinite retry, since the
          if (record.value() == 1) {
            throw new ProcessingErrorRetryException("processing error of record: " + record.key());
          }
          testResults.add(record.value());
        };

    ErrorHandler<String, Integer> errorHandler =
        (record, e, consumer) -> {
          throw new ProcessingErrorRetryException("seems that exception cannot be handled");
        };

    List<MessageListener> listener =
        bundle.createMessageListener(
            MessageListenerRegistration.<String, Integer>builder()
                .withDefaultListenerConfig()
                .forTopic(topics[0])
                .withConsumerConfig(getConsumerConfig())
                .withValueDeserializer(new NoSerializationErrorDeserializer<>(valueDeserializer))
                .withKeyDeserializer(new NoSerializationErrorDeserializer<>(keyDeserializer))
                .withListenerStrategy(
                    new DeadLetterMLS(
                        DROPWIZARD_APP_RULE.getRule().getEnvironment(),
                        handler,
                        errorHandler,
                        new KafkaClientManager(bundle, topics[0]),
                        4,
                        3000))
                .build());

    try (KafkaProducer<String, Integer> producer = getKafkaProducer()) {
      producer.send(new ProducerRecord<>(topics[0], UUID.randomUUID().toString(), 1));
      producer.send(new ProducerRecord<>(topics[0], UUID.randomUUID().toString(), 2));
    }

    await()
        .atMost(15, SECONDS)
        .pollInterval(1, SECONDS)
        .untilAsserted(
            () -> {
              assertThat(
                  "Message with value '2' is processed without errors",
                  testResults.size(),
                  equalTo(1));
              Header[] headers =
                  KAFKA
                      .getKafkaTestUtils()
                      .consumeAllRecordsFromTopic(topics[1])
                      .get(0)
                      .headers()
                      .toArray();

              assertThat(
                  "The Header of the failed messages contains Key Exception",
                  headers[0].key(),
                  equalTo("Exception"));
              assertThat(
                  "The Header of the failed messages contains Value object Exception",
                  Objects.requireNonNull(deserialize(headers[0].value())).getClass().getName(),
                  equalTo(
                      "org.sdase.commons.server.kafka.consumer.strategies.retryprocessingerror.ProcessingErrorRetryException"));
              assertThat(
                  "The Header of the failed messages contains Key Retries",
                  headers[1].key(),
                  equalTo("Retries"));

              assertThat(
                  "The Header of the failed messages contains Value number of Retries",
                  deserialize(headers[1].value()),
                  equalTo(1));
            });

    listener.forEach(MessageListener::stopConsumer);
  }

  @Test
  public void testWithDeletedTopic() {
    String[] topics = calcTopicNames(3);

    List<Integer> testResults = Collections.synchronizedList(new ArrayList<>());

    MessageHandler<String, Integer> handler =
        record -> {
          if (record.value() == 1) {
            throw new ProcessingErrorRetryException("processing error of record: " + record.key());
          }
          testResults.add(record.value());
        };

    ErrorHandler<String, Integer> errorHandler =
        (record, e, consumer) -> {
          throw new ProcessingErrorRetryException("seems that exception cannot be handled");
        };

    List<MessageListener> listener =
        bundle.createMessageListener(
            MessageListenerRegistration.<String, Integer>builder()
                .withDefaultListenerConfig()
                .forTopic(topics[0])
                .withConsumerConfig(getConsumerConfig().setClientId("test-consumer"))
                .withValueDeserializer(new NoSerializationErrorDeserializer<>(valueDeserializer))
                .withKeyDeserializer(new NoSerializationErrorDeserializer<>(keyDeserializer))
                .withListenerStrategy(
                    new DeadLetterMLS(
                        DROPWIZARD_APP_RULE.getRule().getEnvironment(),
                        handler,
                        errorHandler,
                        new KafkaClientManager(bundle, topics[0]),
                        4,
                        1000))
                .build());

    try (KafkaProducer<String, Integer> producer = getKafkaProducer()) {
      producer.send(new ProducerRecord<>(topics[0], UUID.randomUUID().toString(), 1));
      producer.send(new ProducerRecord<>(topics[0], UUID.randomUUID().toString(), 2));
    }

    await()
        .atMost(15, SECONDS)
        .pollInterval(1, SECONDS)
        .untilAsserted(
            () -> {
              assertThat(
                  "Message with value '2' is processed without errors",
                  testResults.size(),
                  equalTo(1));
              assertThat(
                  "there is 1 entry in dead letter topic",
                  KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(topics[2]).size(),
                  equalTo(1));
            });

    listener.forEach(MessageListener::stopConsumer);
  }

  @Test
  public void testWithoutErrorHandler() {
    String[] topics = calcTopicNames(4);
    createTopics(topics);

    AtomicInteger processingError = new AtomicInteger(0);
    List<Integer> testResults = Collections.synchronizedList(new ArrayList<>());

    MessageHandler<String, Integer> handler =
        record -> {
          Integer value = record.value();
          if (value == 2 || value == 10) {
            processingError.incrementAndGet();
            throw new ProcessingErrorRetryException("processing error of record: " + record.key());
          }

          testResults.add(value);
        };

    List<MessageListener> listener =
        bundle.createMessageListener(
            MessageListenerRegistration.<String, Integer>builder()
                .withDefaultListenerConfig()
                .forTopic(topics[0])
                .withConsumerConfig(getConsumerConfig())
                .withValueDeserializer(new NoSerializationErrorDeserializer<>(valueDeserializer))
                .withKeyDeserializer(new NoSerializationErrorDeserializer<>(keyDeserializer))
                .withListenerStrategy(
                    new DeadLetterMLS(
                        DROPWIZARD_APP_RULE.getRule().getEnvironment(),
                        handler,
                        new KafkaClientManager(bundle, topics[0]),
                        4,
                        1000))
                .build());

    try (KafkaProducer<String, Integer> producer = getKafkaProducer()) {
      IntStream.range(1, 21)
          .forEach(
              e -> producer.send(new ProducerRecord<>(topics[0], UUID.randomUUID().toString(), e)));
    }

    await()
        .atMost(15, SECONDS)
        .pollInterval(1, SECONDS)
        .untilAsserted(
            () -> {
              assertThat(
                  "18 messages should be processed successfully", testResults.size(), equalTo(18));

              assertThat(
                  "10 processing errors, 2 error messages with 4 retries each",
                  processingError.get(),
                  equalTo(10));
              assertThat(
                  "There must be 18 results finally processed by consumer (excep 2 and 10)",
                  testResults.size(),
                  equalTo(18));

              assertThat(
                  testResults,
                  containsInAnyOrder(
                      1, 3, 4, 5, 6, 7, 8, 9, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20));

              assertThat(
                  "There are 8 records in the dead letter topic, i.e. four tries for 2 records",
                  KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(topics[1]).size(),
                  equalTo(8));

              assertThat(
                  "2 messages in dead letter",
                  KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(topics[2]).size(),
                  equalTo(2));
            });

    // Test if task copies message back
    triggerAdminTask(topics[2]);

    await()
        .atMost(15, SECONDS)
        .pollInterval(1, SECONDS)
        .untilAsserted(
            () ->
                assertThat(
                    "20 processing errors, 2 additional reinserted error messages with 4 retries each",
                    processingError.get(),
                    equalTo(20)));
    listener.forEach(MessageListener::stopConsumer);
  }

  private void triggerAdminTask(String topic) {
    await()
        .timeout(15, SECONDS)
        .untilAsserted(
            () -> {
              try (Response result =
                  DROPWIZARD_APP_RULE
                      .getRule()
                      .client()
                      .property(ClientProperties.READ_TIMEOUT, 10000)
                      .target(
                          String.format(
                              "http://localhost:%s/tasks/deadLetterResend/%s",
                              DROPWIZARD_APP_RULE.getRule().getAdminPort(), topic))
                      .request()
                      .post(Entity.entity(null, MediaType.APPLICATION_JSON_TYPE))) {
                assertThat(result.getStatus(), equalTo(200));
                assertThat(
                    result.readEntity(String.class),
                    startsWith(String.format("reinserted %s messages", 2)));
              }
            });
  }

  @Test
  public void testDeadLetterMessagesAreNotCopiedMultipleTimes() {
    String[] topics = calcTopicNames(8);
    createTopics(topics);

    // register handler to initialize task
    List<MessageListener> listener =
        bundle.createMessageListener(
            MessageListenerRegistration.<String, Integer>builder()
                .withDefaultListenerConfig()
                .forTopic(topics[0])
                .withConsumerConfig(getConsumerConfig())
                .withValueDeserializer(new NoSerializationErrorDeserializer<>(valueDeserializer))
                .withKeyDeserializer(new NoSerializationErrorDeserializer<>(keyDeserializer))
                .withListenerStrategy(
                    new DeadLetterMLS(
                        DROPWIZARD_APP_RULE.getRule().getEnvironment(),
                        record -> {},
                        new KafkaClientManager(bundle, topics[0]),
                        4,
                        1000))
                .build());

    // insert 2 messages to dead letter
    KAFKA.getKafkaTestUtils().produceRecords(2, topics[2], 0);
    await()
        .atMost(15, SECONDS)
        .pollInterval(1, SECONDS)
        .untilAsserted(
            () ->
                // wait until the two messages are really inserted in dead letter topic
                assertThat(
                    KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(topics[2]).size(),
                    equalTo(2)));

    // 2 messages must be reinserted when admin task is invoked first time
    triggerAdminTask(topics[2]);

    await()
        .atMost(15, SECONDS)
        .pollInterval(1, SECONDS)
        .untilAsserted(
            () ->
                // wait until the two messages are reinserted to dead letter topic
                assertThat(
                    KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(topics[2]).size(),
                    equalTo(4)));

    // reinserted messages must be handled and errors thrown
    triggerAdminTask(topics[2]);

    listener.forEach(MessageListener::stopConsumer);
  }

  @Test
  public void testErrorHandlerReturnsFalseStopsListener() {
    String[] topics = calcTopicNames(5);
    createTopics(topics);

    MessageHandler<String, Integer> handler =
        record -> {
          throw new ProcessingErrorRetryException("processing error of record: " + record.key());
        };

    ErrorHandler<String, Integer> errorHandler = (record, e, consumer) -> false;

    List<MessageListener> listener =
        bundle.createMessageListener(
            MessageListenerRegistration.<String, Integer>builder()
                .withDefaultListenerConfig()
                .forTopic(topics[0])
                .withConsumerConfig(getConsumerConfig())
                .withValueDeserializer(new NoSerializationErrorDeserializer<>(valueDeserializer))
                .withKeyDeserializer(new NoSerializationErrorDeserializer<>(keyDeserializer))
                .withListenerStrategy(
                    new DeadLetterMLS(
                        DROPWIZARD_APP_RULE.getRule().getEnvironment(),
                        handler,
                        errorHandler,
                        new KafkaClientManager(bundle, topics[0]),
                        4,
                        1000))
                .build());

    try (KafkaProducer<String, Integer> producer = getKafkaProducer()) {
      producer.send(new ProducerRecord<>(topics[0], UUID.randomUUID().toString(), 1));
    }

    await()
        .atMost(15, SECONDS)
        .pollInterval(1, SECONDS)
        .untilAsserted(
            () -> {
              boolean allClosed =
                  listener.stream()
                      .allMatch(
                          l -> {
                            try {
                              l.getConsumer().poll(1);
                              // consumer throws IllegalStateException if it is already closed
                            } catch (IllegalStateException e) {
                              return true;
                            } catch (Exception e) {
                              return false;
                            }
                            return false;
                          });
              assertThat("allClosed", allClosed);
            });
  }

  @Test
  public void errorHandlerReturnsTrueAndNoDeadLetterHandlingWillBeInitiated() {
    String[] topics = calcTopicNames(6);
    createTopics(topics);

    AtomicInteger processingError = new AtomicInteger(0);
    List<Integer> testResults = Collections.synchronizedList(new ArrayList<>());

    MessageHandler<String, Integer> handler =
        record -> {
          Integer value = record.value();
          if (value == 2 || value == 10) {
            processingError.incrementAndGet();
            throw new ProcessingErrorRetryException("processing error of record: " + record.key());
          }

          testResults.add(value);
        };

    ErrorHandler<String, Integer> errorHandler = (record, e, consumer) -> true;

    List<MessageListener> listener =
        bundle.createMessageListener(
            MessageListenerRegistration.<String, Integer>builder()
                .withDefaultListenerConfig()
                .forTopic(topics[0])
                .withConsumerConfig(getConsumerConfig())
                .withValueDeserializer(new NoSerializationErrorDeserializer<>(valueDeserializer))
                .withKeyDeserializer(new NoSerializationErrorDeserializer<>(keyDeserializer))
                .withListenerStrategy(
                    new DeadLetterMLS(
                        DROPWIZARD_APP_RULE.getRule().getEnvironment(),
                        handler,
                        errorHandler,
                        new KafkaClientManager(bundle, topics[0]),
                        4,
                        1000))
                .build());

    try (KafkaProducer<String, Integer> producer = getKafkaProducer()) {
      IntStream.range(1, 21)
          .forEach(
              e -> producer.send(new ProducerRecord<>(topics[0], UUID.randomUUID().toString(), e)));
    }

    await()
        .atMost(15, SECONDS)
        .pollInterval(1, SECONDS)
        .untilAsserted(
            () -> {
              assertThat(
                  "18 messages should be processed successfully", testResults.size(), equalTo(18));
              assertThat(
                  "There was at least 1 processing error",
                  processingError.get(),
                  greaterThanOrEqualTo(1));
              assertThat(
                  "There must be 18 results finally processed by consumer (excep 2 and 10)",
                  testResults.size(),
                  equalTo(18));

              assertThat(
                  testResults,
                  containsInAnyOrder(
                      1, 3, 4, 5, 6, 7, 8, 9, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20));

              assertThat(
                  "There are no records in the dead letter topic (record 2 and 10 are lost in the test scenario, the error handler needs to take care of those records if he returns true",
                  KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(topics[1]).size(),
                  equalTo(0));
            });
    listener.forEach(MessageListener::stopConsumer);
  }

  @Test
  public void shouldRetryWhenDeserializationError() {
    String[] topics = calcTopicNames(7);
    createTopics(topics);

    AtomicInteger processingError = new AtomicInteger(0);
    List<Integer> testResults = Collections.synchronizedList(new ArrayList<>());

    MessageHandler<String, Integer> handler = record -> testResults.add(record.value());

    ErrorHandler<String, Integer> errorHandler =
        (record, e, consumer) -> {
          processingError.incrementAndGet();
          throw new ProcessingErrorRetryException("seems that exception cannot be handled");
        };

    List<MessageListener> listener =
        bundle.createMessageListener(
            MessageListenerRegistration.<String, Integer>builder()
                .withDefaultListenerConfig()
                .forTopic(topics[0])
                .withConsumerConfig(getConsumerConfig())
                .withValueDeserializer(new NoSerializationErrorDeserializer<>(valueDeserializer))
                .withKeyDeserializer(new NoSerializationErrorDeserializer<>(keyDeserializer))
                .withListenerStrategy(
                    new DeadLetterMLS(
                        DROPWIZARD_APP_RULE.getRule().getEnvironment(),
                        handler,
                        errorHandler,
                        new KafkaClientManager(bundle, topics[0]),
                        4,
                        500))
                .build());

    try (KafkaProducer<String, String> producer =
        KAFKA
            .getKafkaTestUtils()
            .getKafkaProducer(StringSerializer.class, StringSerializer.class)) {
      producer.send(
          new ProducerRecord<>(
              topics[0], UUID.randomUUID().toString(), "SomeStringThatIsNoInteger"));
    }

    await()
        .atMost(60, SECONDS)
        .pollInterval(1, SECONDS)
        .untilAsserted(
            () -> {
              assertThat(
                  "Non-deserializable messages are not processed in error handler",
                  processingError.get(),
                  equalTo(0));
              assertThat(
                  "Non-deserializable messages are not processed in business handler",
                  testResults.size(),
                  equalTo(0));
              assertThat(
                  "There are records in the retry topic",
                  KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(topics[1]).size(),
                  equalTo(4));

              List<ConsumerRecord<byte[], byte[]>> messages =
                  KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(topics[2]);
              assertThat(
                  "Non-desrializable messages must end in dead letter",
                  messages.size(),
                  equalTo(1));
              messages
                  .get(0)
                  .headers()
                  .headers("Exception")
                  .forEach(
                      e -> {
                        Object v = deserialize(e.value());
                        assertThat(
                            "Serialization Exception must be the cause",
                            v instanceof SerializationException,
                            equalTo(true));
                      });
            });

    listener.forEach(MessageListener::stopConsumer);
  }
}
