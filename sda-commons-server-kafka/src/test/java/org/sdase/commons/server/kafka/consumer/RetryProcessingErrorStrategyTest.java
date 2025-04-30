package org.sdase.commons.server.kafka.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sdase.commons.server.kafka.consumer.strategies.retryprocessingerror.RetryProcessingErrorMLS;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RetryProcessingErrorStrategyTest {
  private static final long MAX_RETRIES = 2;

  private static final TopicPartition TOPIC_PARTITION = new TopicPartition("topic", 0);

  @Mock private MessageHandler<String, String> messageHandler;
  @Mock private ErrorHandler<String, String> intermediateErrorHandler;
  @Mock private ErrorHandler<String, String> finalErrorHandler;
  @Mock private KafkaConsumer<String, String> consumer;

  private RetryProcessingErrorMLS<String, String> strategy;

  @BeforeEach
  void init() {
    when(intermediateErrorHandler.handleError(any(), any(), any())).thenReturn(true);
    when(finalErrorHandler.handleError(any(), any(), any())).thenReturn(true);
    strategy =
        new RetryProcessingErrorMLS<>(
            messageHandler, intermediateErrorHandler, MAX_RETRIES, finalErrorHandler);
  }

  @Test
  void shouldCallMessageHandler() {
    ConsumerRecords<String, String> records = createConsumerRecords(2);
    strategy.processRecords(records, consumer);

    verify(messageHandler).handle(records.records(TOPIC_PARTITION).get(0));
    verify(messageHandler).handle(records.records(TOPIC_PARTITION).get(1));
    verifyNoMoreInteractions(messageHandler);
    verifyNoInteractions(intermediateErrorHandler, finalErrorHandler);
  }

  @Test
  void shouldCallMessageHandlerMultipleTimesOnError() {
    ConsumerRecords<String, String> records = createConsumerRecords(2);
    RuntimeException exception = new RuntimeException("Test");

    // we fail in first call
    doThrow(exception).when(messageHandler).handle(any());
    strategy.processRecords(records, consumer);

    verify(messageHandler).handle(records.records(TOPIC_PARTITION).get(0));
    verify(intermediateErrorHandler)
        .handleError(records.records(TOPIC_PARTITION).get(0), exception, consumer);
    verifyNoMoreInteractions(messageHandler, intermediateErrorHandler);
    verifyNoInteractions(finalErrorHandler);

    // second call is fine
    doNothing().when(messageHandler).handle(any());
    strategy.processRecords(records, consumer);

    verify(messageHandler, times(2)).handle(records.records(TOPIC_PARTITION).get(0));
    verify(messageHandler).handle(records.records(TOPIC_PARTITION).get(1));
    verifyNoMoreInteractions(messageHandler, intermediateErrorHandler);
    verifyNoInteractions(finalErrorHandler);
  }

  @Test
  void shouldStopCallingAfterMaxRetries() {
    ConsumerRecords<String, String> records = createConsumerRecords(2);
    RuntimeException exception = new RuntimeException("Test");

    // we fail in first call
    doThrow(exception).when(messageHandler).handle(any());
    strategy.processRecords(records, consumer);

    verify(messageHandler).handle(records.records(TOPIC_PARTITION).get(0));
    verify(intermediateErrorHandler)
        .handleError(records.records(TOPIC_PARTITION).get(0), exception, consumer);
    verifyNoMoreInteractions(messageHandler, intermediateErrorHandler);
    verifyNoInteractions(finalErrorHandler);

    // we fail in second call
    strategy.processRecords(records, consumer);

    verify(messageHandler, times(2)).handle(records.records(TOPIC_PARTITION).get(0));
    verify(intermediateErrorHandler, times(2))
        .handleError(records.records(TOPIC_PARTITION).get(0), exception, consumer);
    verifyNoMoreInteractions(messageHandler, intermediateErrorHandler);
    verifyNoInteractions(finalErrorHandler);

    // and we fail in third (last) call
    strategy.processRecords(records, consumer);

    verify(messageHandler, times(3)).handle(records.records(TOPIC_PARTITION).get(0));
    verify(intermediateErrorHandler, times(2))
        .handleError(records.records(TOPIC_PARTITION).get(0), exception, consumer);
    verify(finalErrorHandler)
        .handleError(records.records(TOPIC_PARTITION).get(0), exception, consumer);

    // as the first record has been thrown away, the second now can also be processed (due to mock,
    // preparation it will also fail)
    verify(messageHandler).handle(records.records(TOPIC_PARTITION).get(1));
    verify(intermediateErrorHandler)
        .handleError(records.records(TOPIC_PARTITION).get(1), exception, consumer);

    verifyNoMoreInteractions(messageHandler, intermediateErrorHandler);
    verifyNoMoreInteractions(finalErrorHandler);
  }

  static ConsumerRecords<String, String> createConsumerRecords(int noMessages) {
    Map<TopicPartition, List<ConsumerRecord<String, String>>> payload = new HashMap<>();

    String topic = TOPIC_PARTITION.topic();
    List<ConsumerRecord<String, String>> messages = new ArrayList<>();
    for (int i = 0; i < noMessages; i++) {
      messages.add(new ConsumerRecord<>(topic, 0, i, topic, "message-" + i));
    }
    payload.put(TOPIC_PARTITION, messages);
    return new ConsumerRecords<>(payload);
  }
}
