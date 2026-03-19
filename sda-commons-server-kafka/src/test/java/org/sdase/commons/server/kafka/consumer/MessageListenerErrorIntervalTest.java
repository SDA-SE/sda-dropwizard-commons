package org.sdase.commons.server.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.kafka.config.ListenerConfig;
import org.sdase.commons.server.kafka.consumer.strategies.MessageListenerStrategy;

@SuppressWarnings("unchecked")
class MessageListenerErrorIntervalTest {
  static final String TOPIC_NAME = "test";
  MessageListener<String, String> messageListener;
  KafkaConsumer<String, String> consumerMock = mock(KafkaConsumer.class);
  MessageListenerStrategy<String, String> messageListenerStrategyMock =
      mock(MessageListenerStrategy.class);
  Map<TopicPartition, List<ConsumerRecord<String, String>>> recordMock;
  ExecutorService executorService = Executors.newSingleThreadExecutor();

  @BeforeEach
  void setUp() {
    Node nodeMock = mock(Node.class);
    when(consumerMock.partitionsFor(TOPIC_NAME))
        .thenReturn(
            Collections.singletonList(
                new PartitionInfo(
                    TOPIC_NAME, 1, nodeMock, new Node[] {nodeMock}, new Node[] {nodeMock})));
    ConsumerRecord<String, String> singleRecordMock = mock(ConsumerRecord.class);
    recordMock =
        Collections.singletonMap(
            new TopicPartition(TOPIC_NAME, 1), Collections.singletonList(singleRecordMock));
    ListenerConfig listenerConfig =
        ListenerConfig.builder()
            .withPollInterval(1)
            .withMaxPollInterval(10)
            .withPollIntervalFactorOnError(3)
            .withMaxRetries(3)
            .build(1);
    messageListener =
        new MessageListener<>(
            Collections.singletonList(TOPIC_NAME),
            consumerMock,
            listenerConfig,
            messageListenerStrategyMock);
    verify(consumerMock).subscribe(anyCollection());
  }

  @AfterEach
  void tearDown() {
    executorService.shutdown();
  }

  @Test
  void shouldKeepConfiguredIntervalOnSuccess() {
    try {
      when(consumerMock.poll(any())).thenReturn(new ConsumerRecords<>(recordMock));
      executorService.submit(messageListener);
      await()
          .untilAsserted(
              () -> verify(consumerMock, atLeast(10)).poll(Duration.of(1, ChronoUnit.MILLIS)));
    } finally {
      messageListener.stopConsumer();
    }
  }

  @Test
  void shouldIncreaseWaitTimeBetweenPollsOnError() throws Exception {
    when(consumerMock.poll(any())).thenReturn(new ConsumerRecords<>(recordMock));
    doThrow(new RuntimeException())
        .when(messageListenerStrategyMock)
        .processRecords(any(), eq(consumerMock));

    executorService.submit(messageListener);

    // wait for at least three failing invocations
    await()
        .untilAsserted(
            () ->
                verify(messageListenerStrategyMock, atLeast(3))
                    .processRecords(any(), eq(consumerMock)));

    assertThat(getPollPauseInterval()).isEqualTo(10L);
    messageListener.stopConsumer();
  }

  private long getPollPauseInterval() throws Exception {
    Field field = MessageListener.class.getDeclaredField("pollPauseInterval");
    field.setAccessible(true);
    AtomicLong pollPauseInterval = (AtomicLong) field.get(messageListener);
    return pollPauseInterval.get();
  }

  @Test
  void shouldIncreaseIntervalOnErrorAndResetOnSuccess() throws Exception {
    try {
      when(consumerMock.poll(any())).thenReturn(new ConsumerRecords<>(recordMock));
      doThrow(new RuntimeException())
          .doThrow(new RuntimeException())
          .doThrow(new RuntimeException())
          .doThrow(new RuntimeException())
          .doThrow(new RuntimeException())
          .doNothing()
          .when(messageListenerStrategyMock)
          .processRecords(any(), eq(consumerMock));
      executorService.submit(messageListener);

      // wait for at least three failing invocations
      await()
          .untilAsserted(
              () ->
                  verify(messageListenerStrategyMock, atLeast(4))
                      .processRecords(any(), eq(consumerMock)));

      assertThat(getPollPauseInterval()).isEqualTo(1L);
      messageListener.stopConsumer();
    } finally {
      messageListener.stopConsumer();
    }
  }
}
