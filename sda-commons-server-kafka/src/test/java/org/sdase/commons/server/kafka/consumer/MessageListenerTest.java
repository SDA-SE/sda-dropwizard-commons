package org.sdase.commons.server.kafka.consumer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.AdditionalMatchers.gt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.when;

import java.lang.Thread.State;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.awaitility.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import org.sdase.commons.server.kafka.config.ListenerConfig;
import org.sdase.commons.server.kafka.consumer.strategies.legacy.LegacyMLS;
import org.sdase.commons.server.kafka.prometheus.ConsumerTopicMessageHistogram;

public class MessageListenerTest {

   private static final String[] TOPICS = { "create", "delete", "update" };

   private MessageListener<String, String> listener;

   private MessageHandler<String, String> handler;

   private ErrorHandler<String, String> errorHandler;

   private KafkaConsumer<String, String> consumer;

   private ConsumerTopicMessageHistogram histogram;

   private static final int WAIT_TIME_MS = 5000;
   private static final int BLOCKING_TIME_MS = 10000;
   private static final int N_MESSAGES = 5;

   private Thread listenerThread;

   @SuppressWarnings("unchecked")
   @Before
   public void setup() {
      consumer = Mockito.mock(KafkaConsumer.class);
      handler = Mockito.mock(MessageHandler.class);
      errorHandler = Mockito.mock(ErrorHandler.class);
      histogram = Mockito.mock(ConsumerTopicMessageHistogram.class);
   }

   @After
   public void stop() throws InterruptedException {
      try {
         // stop the consumer
         if (listener != null) {
            listener.stopConsumer();
         }

         // wait for the thread to terminate
         if (listenerThread != null) {
            listenerThread.join();
         }
      } finally {
         listenerThread = null;
      }
   }

   private void setupListener() {
      setupListener(0);
   }

   private void setupListener(int topicWaitTime) {
      ListenerConfig lc = ListenerConfig.builder().withTopicMissingRetryMs(topicWaitTime).useAutoCommitOnly(false).build(1);

      MessageHandlerRegistration<String, String> registration = MessageHandlerRegistration.<String, String>builder()
          .withListenerConfig(lc)
          .forTopics(Arrays.asList(TOPICS))
          .withDefaultConsumer()
          .withHandler(handler)
          .withErrorHandler(errorHandler)
          .build();

      LegacyMLS<String, String> strategy = new LegacyMLS<>(
          registration.getHandler(),
          registration.getErrorHandler(),
          lc.isUseAutoCommitOnly(), lc.getCommitType()
      );

      strategy.init(histogram);

      listener = new MessageListener<>(
          registration.getTopicsNames(),
          consumer, lc, strategy
      );
   }

   @Test
   public void itShouldSubscribeToAllTopics() {
      setupMocks();
      setupListener();
      when(consumer.poll(gt(-10))).thenAnswer(invocation -> {
         long timeout = invocation.getArgument(0);
         if (timeout > 0) {
            throw new WakeupException();
         }
         return ConsumerRecords.EMPTY;
      });

      startListenerThread();

      Mockito.verify(consumer).subscribe(Arrays.asList(TOPICS));
   }

   @Test
   public void itShouldReenterPollingQueue() {
      setupMocks();
      setupListener();

      AtomicInteger counter = new AtomicInteger(0);
      when(consumer.poll(gt(0L))).thenAnswer(invocation -> {
         if (counter.incrementAndGet() < 2) {
            throw new WakeupException();
         } else {
            return ConsumerRecords.EMPTY;
         }
      });

      startListenerThread();

      Mockito.verify(consumer, timeout(WAIT_TIME_MS).atLeast(2)).poll(gt(0L));
   }

   @Test
   public void consumerWithUseAutocommitOnlyFalseShouldCallCommit() {
      ConsumerRecords<String, String> records = TestHelper.createConsumerRecords(N_MESSAGES, TOPICS);
      setupMocks();
      setupListener();
      when(consumer.poll(gt(0L))).thenReturn(records);

      startListenerThread();

      Mockito.verify(consumer, timeout(WAIT_TIME_MS).atLeastOnce()).commitSync();
   }

   @Test
   public void errorHandlerShouldBeInvokedWhenExceptionButNotStop() {
      ConsumerRecords<String, String> records = TestHelper.createConsumerRecords(N_MESSAGES, TOPICS);
      setupMocks();
      setupListener();
      AtomicInteger count = new AtomicInteger(0);

      when(consumer.poll(gt(0L))).thenReturn(records);

      Mockito.doThrow(new RuntimeException("SampleException")).when(handler).handle(any());

      when(errorHandler.handleError(any(), any(), any())).then(
            invocation -> {
               count.incrementAndGet();
               return true;
      });

      startListenerThread();
      await().pollInterval(new Duration(1, MILLISECONDS)).until(() -> count.get() >= 1);
      Mockito.verify(consumer, Mockito.never()).close();
   }

   @Test
   public void shouldStopWhenErrorHandlerReturnsFalse() {
      ConsumerRecords<String, String> records = TestHelper.createConsumerRecords(N_MESSAGES, TOPICS);
      setupMocks();
      setupListener();
      AtomicInteger count = new AtomicInteger(0);

      when(consumer.poll(gt(0L))).thenReturn(records);
      Mockito.doThrow(new RuntimeException("SampleException")).when(handler).handle(any());

      when(errorHandler.handleError(any(), any(), any())).then(
            (Answer<Boolean>)invocation -> {
               count.incrementAndGet();
               return false;
            });

      startListenerThread();
      await().until(() -> count.get() >= 1);
      Mockito.verify(consumer, Mockito.atLeastOnce()).close();

   }

   @Test
   public void itShouldHandAllRecordsToMessageHandler() {
      ConsumerRecords<String, String> records = TestHelper.createConsumerRecords(N_MESSAGES, TOPICS);
      setupMocks();
      setupListener();

      AtomicBoolean wasReturned = new AtomicBoolean(false);
      when(consumer.poll(gt(0L))).thenAnswer(
            invocation -> !wasReturned.getAndSet(true) ? records : ConsumerRecords.empty());

      startListenerThread();
      await().atMost(BLOCKING_TIME_MS, MILLISECONDS).untilAsserted(() ->
         records.forEach(r ->
               Mockito.verify(handler, atLeastOnce()).handle(r)));
   }

   @Test
   public void itShouldCorrectlyStop() {
      setupMocks();
      setupListener();
      AtomicBoolean throwException = new AtomicBoolean(false);

      Mockito.doAnswer((Answer<Void>) invocation -> {
         throwException.set(true);
         return null;
      }).when(consumer).wakeup();

      when(consumer.poll(gt(0L))).thenAnswer((Answer<Void>) invocation -> {
         await().atMost(BLOCKING_TIME_MS, MILLISECONDS).untilTrue(throwException);
         throw new WakeupException();
      });

      Thread t = startListenerThread();

      // verify and wait until poll has been invoked
      Mockito.verify(consumer, timeout(WAIT_TIME_MS).times(1)).poll(gt(0L));

      listener.stopConsumer();

      Mockito.verify(consumer, timeout(WAIT_TIME_MS).times(1)).wakeup();
      Mockito.verify(consumer, timeout(WAIT_TIME_MS).times(1)).close();

      await().untilAsserted(() -> assertThat(t.getState()).isEqualTo(State.TERMINATED));
   }

   @Test
   public void itShouldCorrectlyStopEvenWhenTopicDoesNotExist() {
      int waitTime = 10000;
      setupMocks();
      setupListener(waitTime);

      when(consumer.partitionsFor(Mockito.anyString())).thenReturn(new LinkedList<>());

      AtomicBoolean throwException = new AtomicBoolean(false);

      Mockito.doAnswer((Answer<Void>) invocation -> {
         throwException.set(true);
         return null;
      }).when(consumer).wakeup();

      Thread t = startListenerThread();

      // verify and wait until poll has been invoked
      Mockito.verify(consumer, timeout(WAIT_TIME_MS).atLeast(3)).partitionsFor(Mockito.anyString());

      t.interrupt();
      listener.stopConsumer();

      Mockito.verify(consumer, timeout(WAIT_TIME_MS).times(1)).wakeup();
      Mockito.verify(consumer, timeout(WAIT_TIME_MS).times(1)).close();

      await().untilAsserted(() -> assertThat(t.getState()).isEqualTo(State.TERMINATED));
   }

   private Thread startListenerThread() {
      listenerThread = new Thread(listener);
      listenerThread.start();

      return listenerThread;
   }

   private void setupMocks() {
      Mockito.doNothing().when(consumer).subscribe(Mockito.anyList());
      when(consumer.poll(0)).thenReturn(null);
   }
}
