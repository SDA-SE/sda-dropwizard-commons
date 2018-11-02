package com.sdase.commons.server.kafka.consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import com.sdase.commons.server.kafka.config.ListenerConfig;
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
   private static final int WAIT_TIME_INC = 50;
   private static final int BLOCKING_TIME_MS = 10000;
   private static final int N_MESSAGES = 5;

   @SuppressWarnings("unchecked")
   @Before
   public void setup() {
      consumer = Mockito.mock(KafkaConsumer.class);
      handler = Mockito.mock(MessageHandler.class);
   }

   private void setupListener() {
      ListenerConfig lc = ListenerConfig.builder().useAutoCommitOnly(false).build(1);

      HashMap<String, String> map = new HashMap<>();
      map.put("bootstrap.servers", "localhost:8080");
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
   public void itShouldReenterPollingQueue() throws InterruptedException {
      setupMocks();
      setupListener();

      AtomicInteger counter = new AtomicInteger(0);
      Mockito.when(consumer.poll(Mockito.longThat(longGtZero))).thenAnswer(new Answer<Void>() {

         @Override
         public Void answer(InvocationOnMock invocation) throws Throwable {
            if (counter.incrementAndGet() < 2) {
               throw new WakeupException();
            } else {
               return null;
            }
         }
      });

      Thread t = startListenerThread();

      Thread.sleep(WAIT_TIME_MS);

      Mockito.verify(consumer, Mockito.times(2)).poll(Mockito.longThat(longGtZero));

      listener.stopConsumer();

      Thread.sleep(WAIT_TIME_MS);

      assertThat(t.getState(), equalTo(State.TERMINATED));
   }

   @Test
   public void transactionalConsumerShouldCallCommit() throws InterruptedException {
      ConsumerRecords<String, String> records = createConsumerRecords();
      setupMocks();
      setupListener();
      Mockito.when(consumer.poll(Mockito.longThat(longGtZero))).thenReturn(records);

      startListenerThread();
      Thread.sleep(WAIT_TIME_MS);

      Mockito.verify(consumer, Mockito.atLeastOnce()).commitSync();
   }

   @Test
   public void transactionalConsumerShouldNotCallCommitWhenExceptionIsThrown() throws InterruptedException {
      ConsumerRecords<String, String> records = createConsumerRecords();
      setupMocks();
      setupListener();

      Mockito.when(consumer.poll(Mockito.longThat(longGtZero))).then(new Answer<ConsumerRecords<String, String>>() {

         @Override
         public ConsumerRecords<String, String> answer(InvocationOnMock invocation) throws Throwable {
            listener.stopConsumer();

            return records;
         }

      });

      Mockito.doThrow(new KafkaMessageHandlingException()).when(handler).handle(Mockito.anyObject());

      startListenerThread();
      Thread.sleep(WAIT_TIME_MS);

      Mockito.verify(consumer, Mockito.never()).commitSync();

   }

   @Test
   public void itShouldHandAllRecordsToMessageHandler() throws InterruptedException {
      List<ConsumerRecord<String, String>> allRecords = new ArrayList<>();
      ConsumerRecords<String, String> records = createConsumerRecords();
      setupMocks();
      setupListener();
      Mockito.when(consumer.poll(Mockito.longThat(longGtZero))).thenReturn(records);

      startListenerThread();
      Thread.sleep(WAIT_TIME_MS);

      allRecords.forEach(r -> {
         try {
            Mockito.verify(handler).handle(r);
         } catch (Exception e) {
            e.printStackTrace();
         }
      });
   }

   @Test
   public void itShouldCorrectlyStop() throws InterruptedException {
      setupMocks();
      setupListener();
      AtomicBoolean throwException = new AtomicBoolean(false);

      Mockito.doAnswer(new Answer<Void>() {

         @Override
         public Void answer(InvocationOnMock invocation) throws Throwable {
            throwException.set(true);
            return null;
         }

      }).when(consumer).wakeup();

      Mockito.doAnswer(new Answer<Void>() {

         @Override
         public Void answer(InvocationOnMock invocation) throws Throwable {
            int count = 0;
            while (count < BLOCKING_TIME_MS && !throwException.get()) {
               count += WAIT_TIME_INC;
               Thread.sleep(WAIT_TIME_INC);
            }
            throw new WakeupException();
         }
      }).when(consumer).poll(Mockito.longThat(longGtZero));

      Thread t = startListenerThread();
      Thread.sleep(WAIT_TIME_MS);

      listener.stopConsumer();

      Thread.sleep(WAIT_TIME_MS);

      Mockito.verify(consumer, Mockito.times(1)).wakeup();
      Mockito.verify(consumer, Mockito.times(1)).close();
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

      ConsumerRecords<String, String> records = new ConsumerRecords<>(payload);

      return records;
   }
}
