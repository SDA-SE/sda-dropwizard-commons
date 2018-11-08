package org.sdase.commons.server.kafka.consumer;

import org.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import org.sdase.commons.server.kafka.config.ListenerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Generic class to handle incoming Kafka Messages
 * 
 * @author Tim Braun <tim.braun@de.ibm.com>
 *
 * @param <K>
 *           Generic type of Key
 * @param <V>
 *           Generic type of Value
 */
public class MessageListener<K, V> implements Runnable {

   private final long pollInterval;
   private final long topicMissingRetryMs;



   public enum CommitType {
      SYNC, ASYNC
   }
   private static final Logger LOGGER = LoggerFactory.getLogger(MessageListener.class);

   private final MessageHandler<K, V> handler;

   private final Collection<String> topics;

   private final AtomicBoolean shouldStop = new AtomicBoolean(false);

   private final KafkaConsumer<K, V> consumer;

   private final boolean autoCommitOnly;

   private final CommitType commitType;

   public MessageListener(MessageHandlerRegistration<K,V> registration, KafkaConsumer<K,V> consumer, ListenerConfig listenerConfig) {
      this.topics = registration.getTopicsNames();
      this.consumer = consumer;
      consumer.subscribe(topics);
      this.handler = registration.getHandler();
      this.pollInterval = listenerConfig.getPollInterval();
      this.autoCommitOnly = listenerConfig.isUseAutoCommitOnly();
      this.commitType = listenerConfig.getCommitType();
      this.topicMissingRetryMs = listenerConfig.getTopicMissingRetryMs();
   }


   @Override
   public void run() {
      shouldStop.set(false);
      String joinedTopics = String.join(",", topics);

      // Consumer waits until the topic is up, since the KafkaConsumer.poll call floods log file with warnings
      // see https://issues.apache.org/jira/browse/KAFKA-4164
      if (topicMissingRetryMs > 0) {
         while (!shouldStop.get() && topics.stream().map(consumer::partitionsFor).flatMap(Collection::stream).filter(Objects::nonNull).collect(Collectors.toSet()).isEmpty()) {
            LOGGER.warn("Topics {} are not ready yet. Waiting {} ms for retry", joinedTopics, topicMissingRetryMs);
            try {
               Thread.sleep(topicMissingRetryMs);
            } catch (InterruptedException e) {
               LOGGER.error("Thread interrupted when waiting for topic to come up");
               Thread.currentThread().interrupt();
            }
         }
      }


      while (!shouldStop.get()) {
         // return immediately and resubmit Runnable
         try {
            LOGGER.debug("Attaching to Topics [{}]", joinedTopics);

            ConsumerRecords<K, V> records = consumer.poll(pollInterval);
            LOGGER.debug("Received {} messages from topics [{}]", records.count(), joinedTopics);

            processRecords(records);

         } catch (WakeupException w) {
            LOGGER.warn("Woke up before polling returned", w);
         } catch (RuntimeException re) {
            LOGGER.error("Unauthorized or other runtime exception. Cannot poll from [{}]", topics, re);
            break;
         }
      }
      LOGGER.info("MessageListener closing Consumer for [{}]", joinedTopics);
      try {
         if (autoCommitOnly) {
            // try to commit explicitly since waiting for auto commit may be lost due to closing the consumer
            consumer.commitSync();
         }
      } finally {
         consumer.close();
      }
   }

   /**
    * processes consumer records
    * @param records records to process
    */
   private void processRecords(ConsumerRecords<K, V> records) {
      try {

         for (ConsumerRecord<K, V> record : records) {
            LOGGER.debug("Handling message for {}", record.key());
            handler.handle(record);
         }

         if (!autoCommitOnly && records.count() > 0) {
            if (commitType == CommitType.SYNC) {
               consumer.commitSync();
            } else {
               if (!(handler instanceof CallbackMessageHandler)) {
                  consumer.commitAsync();
               } else {
                  consumer.commitAsync(this::handleOnComplete);
               }
            }
         }

      } catch (KafkaMessageHandlingException kmhe) {
         LOGGER.error("Error while executing future. Exception was: ", kmhe);
      }
   }

   private void handleOnComplete(Map<TopicPartition, OffsetAndMetadata> offsets, Exception exception) {
      if (LOGGER.isDebugEnabled()) {
         LOGGER
               .debug("Handling callback for topic partition {}",
                     offsets
                           .keySet()
                           .stream()
                           .map(x -> String.format("[%s]", x.topic()))
                           .collect(Collectors.joining()));
      }
      if (handler instanceof CallbackMessageHandler) {
         try {
            ((CallbackMessageHandler) handler).handleCommitCallback(offsets, exception);
         } catch (Exception e) {
            LOGGER.error("Error handling callback message!", e);
         }
      } else {
         LOGGER.warn("Compensation invoked, but handler is not an instance of CallbackMessageHandler");
      }
   }

   /**
    * Method used to stop listener running in different thread. This is non
    * blocking. You should use Thread.join() (or something like that) to ensure
    * the thread is actually shutting down.
    */
   public void stopConsumer() {
      shouldStop.set(true);

      if (consumer != null) {
         consumer.wakeup();
      }
   }

   @Override
   public String toString() {
      return "ML ".concat(String.join("", topics));
   }

}
