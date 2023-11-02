package org.sdase.commons.server.kafka.consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.when;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sdase.commons.server.kafka.consumer.strategies.synccommit.SyncCommitErrorHandler;
import org.sdase.commons.server.kafka.consumer.strategies.synccommit.SyncCommitMLS;
import org.sdase.commons.server.kafka.prometheus.ConsumerTopicMessageHistogram;

class SyncCommitStrategyTest {

  private MessageHandler<String, String> handler;
  private ErrorHandler<String, String> errorHandler;
  private SyncCommitErrorHandler<String, String> syncCommitErrorHandler;
  private KafkaConsumer<String, String> consumer;
  private ConsumerTopicMessageHistogram histogram;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setup() {
    consumer = Mockito.mock(KafkaConsumer.class);
    handler = Mockito.mock(MessageHandler.class);
    errorHandler = Mockito.mock(ErrorHandler.class);
    syncCommitErrorHandler = Mockito.mock(SyncCommitErrorHandler.class);
    histogram = Mockito.mock(ConsumerTopicMessageHistogram.class);
  }

  @Test
  void shouldInvokeCommitAfterEachChunkOfRecords() {
    SyncCommitMLS<String, String> strategy = new SyncCommitMLS<>(handler, errorHandler);
    strategy.init(histogram);

    strategy.processRecords(TestHelper.createConsumerRecords(5, "topic"), consumer);
    Mockito.verify(consumer, timeout(100).times(1)).commitSync();
    Mockito.verify(histogram, timeout(100).times(5)).observe(anyDouble(), anyString(), anyString());

    // second chunk commits again
    strategy.processRecords(TestHelper.createConsumerRecords(5, "topic"), consumer);
    Mockito.verify(consumer, timeout(100).times(2)).commitSync();
    Mockito.verify(histogram, timeout(100).times(10))
        .observe(anyDouble(), anyString(), anyString());
  }

  @Test
  void shouldInvokeErrorHandlerWhenException() {
    doThrow(new RuntimeException("Test")).when(handler).handle(any());
    when(errorHandler.handleError(any(), any(), any())).thenReturn(true);

    SyncCommitMLS<String, String> strategy = new SyncCommitMLS<>(handler, errorHandler);
    strategy.init(histogram);

    strategy.processRecords(TestHelper.createConsumerRecords(5, "topic"), consumer);
    Mockito.verify(consumer, timeout(100).times(1)).commitSync();
    Mockito.verify(handler, timeout(100).times(5)).handle(any());
    Mockito.verify(errorHandler, timeout(100).times(5)).handleError(any(), any(), any());
  }

  @Test
  void shouldInvokeSyncCommitErrorHandlerWhenException() {
    doThrow(new TimeoutException()).when(consumer).commitSync();

    SyncCommitMLS<String, String> strategy =
        new SyncCommitMLS<>(handler, errorHandler, syncCommitErrorHandler);
    strategy.init(histogram);
    strategy.processRecords(TestHelper.createConsumerRecords(5, "topic"), consumer);

    Mockito.verify(consumer, timeout(100).times(1)).commitSync();
    Mockito.verify(syncCommitErrorHandler, timeout(100).times(1)).handleError(any(), any());
  }

  @Test
  void shouldStillThrowOtherExceptionsIfNoSyncCommitErrorHandlerConfigured() {
    doThrow(new TimeoutException()).when(consumer).commitSync();

    SyncCommitMLS<String, String> strategy = new SyncCommitMLS<>(handler, errorHandler);
    strategy.init(histogram);

    assertThrows(
        TimeoutException.class,
        () -> strategy.processRecords(TestHelper.createConsumerRecords(5, "topic"), consumer));

    Mockito.verify(consumer, timeout(100).times(1)).commitSync();
  }
}
