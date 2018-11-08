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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import org.sdase.commons.server.kafka.config.ListenerConfig;

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

   private KafkaConsumer<String, String> consumer;



   private static final int WAIT_TIME_MS = 1000;
   private static final int BLOCKING_TIME_MS = 10000;
   private static final int N_MESSAGES = 5;

   @SuppressWarnings("unchecked")
   @Before
   public void setup() {
      consumer = Mockito.mock(KafkaConsumer.class);
      handler = Mockito.mock(MessageHandler.class);
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
                  .build()
            , consumer, lc
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


      Mockito.verify(consumer, timeout(WAIT_TIME_MS).times(2)).poll(Mockito.longThat(longGtZero));

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
   public void transactionalConsumerShouldNotCallCommitWhenExceptionIsThrown()  {
      ConsumerRecords<String, String> records = createConsumerRecords();
      setupMocks();
      setupListener();

      Mockito.when(consumer.poll(Mockito.longThat(longGtZero))).then((Answer<ConsumerRecords<String, String>>) invocation -> {
         listener.stopConsumer();

         return records;
      });

      Mockito.doThrow(new KafkaMessageHandlingException()).when(handler).handle(Mockito.anyObject());

      startListenerThread();
      await().atMost(WAIT_TIME_MS, MILLISECONDS);

      Mockito.verify(consumer, Mockito.never()).commitSync();

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

      records.forEach(r -> {
         Mockito.verify(handler, atLeastOnce()).handle(r);
      });

   }

   @Test
   public void itShouldCorrectlyStop() {
      setupMocks();
      setupListener();
      AtomicBoolean throwException = new AtomicBoolean(false);

      Mockito.doAnswer(new Answer<Void>() {

         @Override
         public Void answer(InvocationOnMock invocation) {
            throwException.set(true);
            return null;
         }

      }).when(consumer).wakeup();

      Mockito.doAnswer(new Answer<Void>() {

         @Override
         public Void answer(InvocationOnMock invocation) {
            await().atMost(BLOCKING_TIME_MS, MILLISECONDS).untilTrue(throwException);
            throw new WakeupException();
         }
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

      Mockito.doAnswer(new Answer<Void>() {

         @Override
         public Void answer(InvocationOnMock invocation) {
            throwException.set(true);
            return null;
         }

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
