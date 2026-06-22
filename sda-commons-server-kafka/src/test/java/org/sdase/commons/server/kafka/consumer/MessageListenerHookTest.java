package org.sdase.commons.server.kafka.consumer;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.sdase.commons.server.kafka.config.ListenerConfig;
import org.sdase.commons.server.kafka.consumer.strategies.MessageListenerStrategy;

@SuppressWarnings("unchecked")
class MessageListenerHookTest {

  static final String TOPIC_NAME = "test-hook-topic";

  KafkaConsumer<String, String> consumerMock;
  MessageListenerStrategy<String, String> strategyMock;
  MessageListener<String, String> messageListener;
  ExecutorService executorService;

  @BeforeEach
  void setUp() {
    consumerMock = mock(KafkaConsumer.class);
    strategyMock = mock(MessageListenerStrategy.class);
    executorService = Executors.newSingleThreadExecutor();

    when(consumerMock.partitionsFor(anyString())).thenReturn(Collections.singletonList(mock(PartitionInfo.class)));
    when(consumerMock.poll(any(Duration.class))).thenReturn(ConsumerRecords.empty());

    messageListener = new MessageListener<>(
        Collections.singletonList(TOPIC_NAME),
        consumerMock,
        ListenerConfig.getDefault(),
        strategyMock);

    verify(consumerMock).subscribe(anyCollection());
  }

  @AfterEach
  void tearDown() {
    messageListener.stopConsumer();
    executorService.shutdown();
  }

  @Test
  void shouldCallBeforePollHookInEachCycle() {
    try {
      executorService.submit(messageListener);

      await()
          .atMost(Duration.ofSeconds(5))
          .untilAsserted(() -> verify(strategyMock, atLeast(5)).beforePoll(consumerMock));

    } finally {
      messageListener.stopConsumer();
    }
  }

  @Test
  void shouldCallBeforePollHookBeforePolling() {
    try {
      InOrder inOrder = inOrder(strategyMock, consumerMock);
      executorService.submit(messageListener);

      await()
          .atMost(Duration.ofSeconds(5))
          .untilAsserted(() -> {
            inOrder.verify(strategyMock).beforePoll(consumerMock);
            inOrder.verify(consumerMock).poll(any(Duration.class));
          });

    } finally {
      messageListener.stopConsumer();
    }
  }

  @Test
  void shouldExecuteActionsWithinHook() {
    doAnswer(invocation -> {
      KafkaConsumer<String, String> consumer = invocation.getArgument(0);
      consumer.seek(new TopicPartition(TOPIC_NAME, 0), 123L);
      return null;
    }).when(strategyMock).beforePoll(consumerMock);

    try {
      executorService.submit(messageListener);

      await()
          .atMost(Duration.ofSeconds(5))
          .untilAsserted(() -> verify(consumerMock, atLeastOnce()).seek(any(TopicPartition.class), anyLong()));

      InOrder inOrder = inOrder(strategyMock, consumerMock);
      inOrder.verify(strategyMock).beforePoll(consumerMock);
      inOrder.verify(consumerMock).seek(new TopicPartition(TOPIC_NAME, 0), 123L);
      inOrder.verify(consumerMock).poll(any(Duration.class));

    } finally {
      messageListener.stopConsumer();
    }
  }
}
