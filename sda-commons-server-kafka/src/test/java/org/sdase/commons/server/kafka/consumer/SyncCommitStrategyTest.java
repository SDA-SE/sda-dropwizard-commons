package org.sdase.commons.server.kafka.consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sdase.commons.server.kafka.consumer.strategies.synccommit.SyncCommitErrorHandler;
import org.sdase.commons.server.kafka.consumer.strategies.synccommit.SyncCommitMLS;

class SyncCommitStrategyTest {

  private MessageHandler<String, String> handler;
  private ErrorHandler<String, String> errorHandler;
  private SyncCommitErrorHandler<String, String> syncCommitErrorHandler;
  private KafkaConsumer<String, String> consumer;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setup() {
    consumer = Mockito.mock(KafkaConsumer.class);
    handler = Mockito.mock(MessageHandler.class);
    errorHandler = Mockito.mock(ErrorHandler.class);
    syncCommitErrorHandler = Mockito.mock(SyncCommitErrorHandler.class);
  }

  @Test
  void shouldInvokeCommitAfterEachChunkOfRecords() {
    SyncCommitMLS<String, String> strategy = new SyncCommitMLS<>(handler, errorHandler);
    strategy.init(null);

    strategy.processRecords(TestHelper.createConsumerRecords(5, "topic"), consumer);
    Mockito.verify(consumer, timeout(100).times(1)).commitSync();

    // second chunk commits again
    strategy.processRecords(TestHelper.createConsumerRecords(5, "topic"), consumer);
    Mockito.verify(consumer, timeout(100).times(2)).commitSync();
  }

  @Test
  void shouldInvokeErrorHandlerWhenException() {
    doThrow(new RuntimeException("Test")).when(handler).handle(any());
    when(errorHandler.handleError(any(), any(), any())).thenReturn(true);

    SyncCommitMLS<String, String> strategy = new SyncCommitMLS<>(handler, errorHandler);
    strategy.init(null);

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
    strategy.init(null);
    strategy.processRecords(TestHelper.createConsumerRecords(5, "topic"), consumer);

    Mockito.verify(consumer, timeout(100).times(1)).commitSync();
    Mockito.verify(syncCommitErrorHandler, timeout(100).times(1)).handleError(any(), any());
  }

  @Test
  void shouldStillThrowOtherExceptionsIfNoSyncCommitErrorHandlerConfigured() {
    doThrow(new TimeoutException()).when(consumer).commitSync();

    SyncCommitMLS<String, String> strategy = new SyncCommitMLS<>(handler, errorHandler);
    strategy.init(null);

    assertThrows(
        TimeoutException.class,
        () -> strategy.processRecords(TestHelper.createConsumerRecords(5, "topic"), consumer));

    Mockito.verify(consumer, timeout(100).times(1)).commitSync();
  }

  @Test
  void shouldNotCommitOnEmptyRecords() {
    // given
    SyncCommitMLS<String, String> strategy = new SyncCommitMLS<>(handler, errorHandler);
    strategy.init(null);
    ConsumerRecords<String, String> emptyRecords = new ConsumerRecords<>(Collections.emptyMap());

    // when the strategy processes the empty records
    strategy.processRecords(emptyRecords, consumer);

    // then commitSync() should never be called on the consumer
    Mockito.verify(consumer, Mockito.never()).commitSync();
  }
}
