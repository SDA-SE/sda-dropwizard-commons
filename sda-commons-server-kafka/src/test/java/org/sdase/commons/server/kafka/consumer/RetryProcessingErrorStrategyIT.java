package org.sdase.commons.server.kafka.consumer;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sdase.commons.server.kafka.KafkaBundle;
import org.sdase.commons.server.kafka.KafkaBundleConsts;
import org.sdase.commons.server.kafka.builder.MessageListenerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.config.ConsumerConfig;
import org.sdase.commons.server.kafka.consumer.strategies.retryprocessingerror.RetryProcessingErrorMLS;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;

class RetryProcessingErrorStrategyIT {
  private static final String TOPIC_NAME = "some-topic";

  private static final int MAX_RETRIES = 2;

  @RegisterExtension
  @Order(0)
  public static final SharedKafkaTestResource KAFKA =
      new SharedKafkaTestResource()
          .withBrokers(1)
          .withBrokerProperty("offsets.topic.num.partitions", "3")
          // we don't need to wait that a consumer group rebalances since we always start with a
          // fresh kafka instance
          .withBrokerProperty("group.initial.rebalance.delay.ms", "0");

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<KafkaTestConfiguration> DROPWIZARD_APP_EXTENSION =
      new DropwizardAppExtension<>(
          KafkaTestApplication.class,
          resourceFilePath("test-config-default.yml"),
          config("kafka.brokers", KAFKA::getKafkaConnectString));

  private static KafkaProducer<String, String> stringStringProducer;

  private static MessageHandler<String, String> messageHandler = mock(MessageHandler.class);

  private static ErrorHandler<String, String> errorHandler = mock(ErrorHandler.class);
  private static ErrorHandler<String, String> finalErrorHandler = mock(ErrorHandler.class);

  @BeforeAll
  static void beforeAll() {
    KAFKA.getKafkaTestUtils().createTopic(TOPIC_NAME, 3, Short.parseShort("1"));

    KafkaTestApplication testApplication = DROPWIZARD_APP_EXTENSION.getApplication();
    KafkaBundle<KafkaTestConfiguration> kafkaBundle = testApplication.kafkaBundle();
    kafkaBundle.createMessageListener(
        MessageListenerRegistration.builder()
            .withDefaultListenerConfig()
            .forTopic(TOPIC_NAME)
            .withConsumerConfig(
                ConsumerConfig.builder()
                    .addConfig("max.poll.records", "1")
                    .addConfig("auto.commit.interval.ms", "1")
                    .addConfig("enable.auto.commit", "false")
                    .build())
            .withValueDeserializer(new StringDeserializer())
            .withListenerStrategy(
                new RetryProcessingErrorMLS<>(
                    messageHandler, errorHandler, MAX_RETRIES, finalErrorHandler))
            .build());

    kafkaBundle.registerProducer(
        ProducerRegistration.builder()
            .forTopic(TOPIC_NAME)
            .withDefaultProducer()
            .withKeySerializer(new StringSerializer())
            .withValueSerializer(new StringSerializer())
            .build());

    stringStringProducer =
        KAFKA.getKafkaTestUtils().getKafkaProducer(StringSerializer.class, StringSerializer.class);
  }

  @AfterAll
  static void afterAll() {
    stringStringProducer.close();
  }

  @BeforeEach
  void setup() {
    reset(messageHandler, errorHandler, finalErrorHandler);
    when(errorHandler.handleError(any(), any(), any())).thenReturn(true);
    when(finalErrorHandler.handleError(any(), any(), any())).thenReturn(true);
  }

  @Test
  void shouldStopRetryingAfterMaxRetries() {
    configureFailCountPerMessage(Integer.MAX_VALUE);
    sendMessages(1);

    await()
        .atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS)
        .untilAsserted(
            () -> {
              assertHandlerCalledWithMessage(1, MAX_RETRIES);
              verify(errorHandler, times(MAX_RETRIES)).handleError(any(), any(), any());
              verify(finalErrorHandler, times(1)).handleError(any(), any(), any());
            });
  }

  @Test
  void shouldStopRetryingAfterMaxRetriesWhenUsingMultiplePartitions() {
    int noOfMessages = 50;
    configureFailCountPerMessage(Integer.MAX_VALUE);
    sendMessages(noOfMessages);

    await()
        .atMost(30, SECONDS)
        .untilAsserted(
            () -> {
              assertHandlerCalledWithMessage(noOfMessages, MAX_RETRIES);
              verify(errorHandler, times(MAX_RETRIES * noOfMessages))
                  .handleError(any(), any(), any());
              verify(finalErrorHandler, times(noOfMessages)).handleError(any(), any(), any());
            });
  }

  @Test
  void shouldStopRetryingWhenMessagesIsSuccessfulAndUsingMultiplePartitions() {
    int failCount = 1;
    int noOfMessages = 20;

    configureFailCountPerMessage(failCount);
    sendMessages(noOfMessages);

    await()
        .atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS)
        .untilAsserted(
            () -> {
              assertHandlerCalledWithMessage(noOfMessages, failCount);
              verify(errorHandler, times(noOfMessages * failCount))
                  .handleError(any(), any(), any());
              verifyNoInteractions(finalErrorHandler);
            });
  }

  /**
   * Configure the messageHandler mock to fail each message a given number of times before
   * succeeding
   */
  private void configureFailCountPerMessage(int failCount) {
    Map<String, Integer> errorCount = new HashMap<>();
    doAnswer(
            invocation -> {
              ConsumerRecord<String, String> consumerRecord = invocation.getArgument(0);
              String key = consumerRecord.value();
              Integer myErrors = errorCount.get(key);
              if (myErrors == null) {
                myErrors = 0;
                errorCount.put(key, myErrors);
              }
              if (myErrors < failCount) {
                errorCount.put(key, myErrors + 1);
                throw new RuntimeException("Simulated processing error");
              }
              return null;
            })
        .when(messageHandler)
        .handle(any(ConsumerRecord.class));
  }

  private void assertHandlerCalledWithMessage(int noOfMessages, int failCount) {
    ArgumentCaptor<ConsumerRecord<String, String>> captor =
        ArgumentCaptor.forClass(ConsumerRecord.class);
    Mockito.verify(messageHandler, atLeast(1)).handle(captor.capture());

    List<ConsumerRecord<String, String>> capturedRecords = captor.getAllValues();
    List<String> capturedMessages = capturedRecords.stream().map(ConsumerRecord::value).toList();

    List<String> expectedMessages = new ArrayList<>();
    for (int i = 0; i < noOfMessages; i++) {
      for (int j = 0;
          j < failCount + 1;
          j++) { // +1 because the message will be executed as least once
        expectedMessages.add("test message " + i);
      }
    }

    assertThat(capturedMessages).containsExactlyInAnyOrderElementsOf(expectedMessages);
  }

  private void sendMessages(int noOfMessages) {
    for (int i = 0; i < noOfMessages; i++) {
      String key = UUID.randomUUID().toString();
      stringStringProducer.send(new ProducerRecord<>(TOPIC_NAME, key, "test message " + i));
    }
  }
}
