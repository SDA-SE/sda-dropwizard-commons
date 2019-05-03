package org.sdase.commons.server.kafka.consumer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.timeout;

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import org.sdase.commons.server.kafka.config.ListenerConfig;
import org.sdase.commons.server.kafka.prometheus.ConsumerTopicMessageHistogram;

public class MessageListenerTest {


   private static final Matcher<Long> longGtZero = new BaseMatcher<Long>() {

      @Override
      public boolean matches(Object item) {
         if (item instanceof Long) {
            return ((Long) item) > 0;
         }
         return false;
      }

      @Override
      public void describeTo(Description description) {
         //
      }

   };

   private static final String[] TOPICS = { "create", "delete", "update" };

   private MessageListener<String, String> listener;

   private MessageHandler<String, String> handler;

   private ErrorHandler<String, String> errorHandler;

   private KafkaConsumer<String, String> consumer;
   
   private ConsumerTopicMessageHistogram histogram;

   private static final int WAIT_TIME_MS = 5000;
   private static final int BLOCKING_TIME_MS = 10000;
   private static final int N_MESSAGES = 5;

   @SuppressWarnings("unchecked")
   @Before
   public void setup() {
      consumer = Mockito.mock(KafkaConsumer.class);
      handler = Mockito.mock(MessageHandler.class);
      errorHandler = Mockito.mock(ErrorHandler.class);
      histogram = Mockito.mock(ConsumerTopicMessageHistogram.class);
   }

   private void setupListener() {
      setupListener(0);
   }

   private void setupListener(int topicWaitTime) {
      ListenerConfig lc = ListenerConfig.builder().withTopicMissingRetryMs(topicWaitTime).useAutoCommitOnly(false).build(1);

      listener = new MessageListener<>(
            MessageHandlerRegistration.<String, String>builder()
                  .withListenerConfig(lc)
                  .forTopics(Arrays.asList(TOPICS))
                  .withDefaultConsumer()
                  .withHandler(handler)
                  .withErrorHandler(errorHandler)
                  .build()
            , consumer, lc, histogram
      );
   }



   @Test
   public void itShouldSubscribeToAllTopics()  {
      setupMocks();
      setupListener();
      Mockito.when(consumer.poll(Mockito.longThat(longGtZero))).thenThrow(new WakeupException());

      startListenerThread();

      listener.stopConsumer();

      Mockito.verify(consumer).subscribe(Arrays.asList(TOPICS));
   }

   @Test
   public void itShouldReenterPollingQueue() {
      setupMocks();
      setupListener();

      AtomicInteger counter = new AtomicInteger(0);
      Mockito.when(consumer.poll(Mockito.longThat(longGtZero))).thenAnswer((Answer<Void>) invocation -> {
         if (counter.incrementAndGet() < 2) {
            throw new WakeupException();
         } else {
            return null;
         }
      });

      Thread t = startListenerThread();


      Mockito.verify(consumer, timeout(WAIT_TIME_MS).atLeast(2)).poll(Mockito.longThat(longGtZero));

      listener.stopConsumer();
      await().atMost(WAIT_TIME_MS, MILLISECONDS).until(() -> t.getState().equals(State.TERMINATED));
      assertThat(t.getState(), equalTo(State.TERMINATED));
   }

   @Test
   public void consumerWithUseAutocommitOnlyFalseShouldCallCommit() {
      ConsumerRecords<String, String> records = createConsumerRecords();
      setupMocks();
      setupListener();
      Mockito.when(consumer.poll(Mockito.longThat(longGtZero))).thenReturn(records);

      startListenerThread();

      Mockito.verify(consumer, timeout(WAIT_TIME_MS).atLeastOnce()).commitSync();
   }

   @Test
   public void errorHandlerShouldBeInvokedWhenExceptionButNotStop()  {
      ConsumerRecords<String, String> records = createConsumerRecords();
      setupMocks();
      setupListener();
      AtomicInteger count = new AtomicInteger(0);

      Mockito.when(consumer.poll(Mockito.longThat(longGtZero))).thenReturn(records);

      Mockito.doThrow(new RuntimeException("SampleException")).when(handler).handle(Mockito.anyObject());

      Mockito.when(errorHandler.handleError(Mockito.anyObject(), Mockito.anyObject(), Mockito.anyObject())).then(
            (Answer<Boolean>)invocation -> {
               count.incrementAndGet();
               return true;
      });

      startListenerThread();
      await().until(() -> count.get() >= 1);
      Mockito.verify(consumer, Mockito.never()).close();
   }


   @Test
   public void shouldStopWhenErrorHandlerReturnsFalse()  {
      ConsumerRecords<String, String> records = createConsumerRecords();
      setupMocks();
      setupListener();
      AtomicInteger count = new AtomicInteger(0);

      Mockito.when(consumer.poll(Mockito.longThat(longGtZero))).thenReturn(records);
      Mockito.doThrow(new RuntimeException("SampleException")).when(handler).handle(Mockito.anyObject());

      Mockito.when(errorHandler.handleError(Mockito.anyObject(), Mockito.anyObject(), Mockito.anyObject())).then(
            (Answer<Boolean>)invocation -> {
               count.incrementAndGet();
               return false;
            });

      startListenerThread();
      await().until(() -> count.get() >= 1);
      Mockito.verify(consumer, Mockito.atLeastOnce()).close();

   }

   @Test
   public void itShouldHandAllRecordsToMessageHandler()  {

      ConsumerRecords<String, String> records = createConsumerRecords();
      setupMocks();
      setupListener();
      Mockito.when(consumer.poll(Mockito.longThat(longGtZero))).thenReturn(records);

      startListenerThread();
      await().atMost(BLOCKING_TIME_MS, MILLISECONDS).until(() -> {
         AtomicBoolean test = new AtomicBoolean(true);
         records.forEach(r -> {
               try {
                  Mockito.verify(handler, atLeastOnce()).handle(r);
               } catch (Exception e) {
                  test.set(false);
               }

         });
         return test.get();
      });

      records.forEach(r -> Mockito.verify(handler, atLeastOnce()).handle(r));

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

      Mockito.doAnswer((Answer<Void>) invocation -> {
         await().atMost(BLOCKING_TIME_MS, MILLISECONDS).untilTrue(throwException);
         throw new WakeupException();
      }).when(consumer).poll(Mockito.longThat(longGtZero));

      Thread t = startListenerThread();

      //verify and wait until poll has been invoked
      Mockito.verify(consumer, timeout(WAIT_TIME_MS).times(1)).poll(Mockito.longThat(longGtZero));

      listener.stopConsumer();

      Mockito.verify(consumer, timeout(WAIT_TIME_MS).times(1)).wakeup();
      Mockito.verify(consumer, timeout(WAIT_TIME_MS).times(1)).close();
      assertThat(t.getState(), equalTo(State.TERMINATED));
   }


   @Test
   public void itShouldCorrectlyStopEvenWhenTopicDoesNotExist() {
      int waitTime = 10000;
      setupMocks();
      setupListener(waitTime);

      Mockito.when(consumer.partitionsFor(Mockito.anyString())).thenReturn(new LinkedList<>());


      AtomicBoolean throwException = new AtomicBoolean(false);

      Mockito.doAnswer((Answer<Void>) invocation -> {
         throwException.set(true);
         return null;
      }).when(consumer).wakeup();


      Thread t = startListenerThread();

      //verify and wait until poll has been invoked
      Mockito.verify(consumer, timeout(WAIT_TIME_MS).atLeast(3)).partitionsFor(Mockito.anyString());

      t.interrupt();
      listener.stopConsumer();

      Mockito.verify(consumer, timeout(WAIT_TIME_MS).times(1)).wakeup();
      Mockito.verify(consumer, timeout(WAIT_TIME_MS).times(1)).close();
      assertThat(t.getState(), equalTo(State.TERMINATED));
   }

   private Thread startListenerThread() {
      Thread t = new Thread(listener);
      t.start();

      return t;
   }

   @SuppressWarnings("unchecked")
   private void setupMocks() {
      Mockito.doNothing().when(consumer).subscribe(Mockito.anyList());
      Mockito.when(consumer.poll(0)).thenReturn(null);
   }

   private static ConsumerRecords<String, String> createConsumerRecords() {
      Map<TopicPartition, List<ConsumerRecord<String, String>>> payload = new HashMap<>();

      for (String topic : TOPICS) {
         TopicPartition tp = new TopicPartition(topic, 0);

         List<ConsumerRecord<String, String>> messages = new ArrayList<>();
         for (int i = 0; i < N_MESSAGES; i++) {
            ConsumerRecord<String, String> cr = new ConsumerRecord<>(topic, 0, 0, topic, UUID.randomUUID().toString());

            messages.add(cr);
         }

         payload.put(tp, messages);
      }

      return new ConsumerRecords<>(payload);

   }
}
