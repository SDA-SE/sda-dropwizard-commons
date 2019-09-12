package org.sdase.commons.server.kafka.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.when;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sdase.commons.server.kafka.consumer.strategies.synccommit.SyncCommitMLS;
import org.sdase.commons.server.kafka.prometheus.ConsumerTopicMessageHistogram;

public class SyncCommitStrategyTest {

   private MessageHandler<String, String> handler;
   private ErrorHandler<String, String> errorHandler;
   private KafkaConsumer<String, String> consumer;
   private ConsumerTopicMessageHistogram histogram;

   @SuppressWarnings("unchecked")
   @Before
   public void setup() {
      consumer = Mockito.mock(KafkaConsumer.class);
      handler = Mockito.mock(MessageHandler.class);
      errorHandler = Mockito.mock(ErrorHandler.class);
      histogram = Mockito.mock(ConsumerTopicMessageHistogram.class);
   }

   @Test
   public void shouldInvokeCommitAfterEachChunkOfRecords() {
      SyncCommitMLS<String, String> strategy = new SyncCommitMLS<>(handler, errorHandler);
      strategy.init(histogram);

      strategy.processRecords(TestHelper.createConsumerRecords(5, "topic"), consumer);
      Mockito.verify(consumer, timeout(100).times(1)).commitSync();
      Mockito.verify(histogram, timeout(100).times(5)).observe(anyDouble(), anyString(), anyString());

      // second chunk commits again
      strategy.processRecords(TestHelper.createConsumerRecords(5, "topic"), consumer);
      Mockito.verify(consumer, timeout(100).times(2)).commitSync();
      Mockito.verify(histogram, timeout(100).times(10)).observe(anyDouble(), anyString(), anyString());
   }

  @Test
  public void shouldInvokeErrorHandlerWhenException() {
    doThrow(new RuntimeException("Test")).when(handler).handle(any());
    when(errorHandler.handleError(any(), any(), any())).thenReturn(true);

    SyncCommitMLS<String, String> strategy = new SyncCommitMLS<>(handler, errorHandler);
    strategy.init(histogram);

    strategy.processRecords(TestHelper.createConsumerRecords(5, "topic"), consumer);
    Mockito.verify(consumer, timeout(100).times(1)).commitSync();
    Mockito.verify(handler, timeout(100).times(5)).handle(any());
    Mockito.verify(errorHandler, timeout(100).times(5)).handleError(any(), any(), any());

  }

}
