package org.sdase.commons.server.kafka.consumer;

import org.apache.kafka.clients.consumer.CommitFailedException;
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
 * <p>
 * A MessageListener implements a default polling loop for retrieving messages
 * from one to many kafka topics. It is configured by
 * the @{@link ListenerConfig} parameters, such as the commitType, the polling
 * interval and an wait interval if the topic is not available before entering
 * the polling loop.
 * </p>
 * <p>
 * The listener requires a {@link KafkaConsumer} to connect to Kafka.
 * Additionally, it requires a {@link MessageHandler} for the business logic
 * for processing a read record and an {@link ErrorHandler} for business logic
 * in error case. The error will be logged in any case.
 * The {@link IgnoreAndProceedErrorHandler} can be used if nothing must be done
 * in error case.
 * </p>
 * <p>
 * If {@link CommitType} async is selected, a {@link CallbackMessageHandler}
 * provides an additional method for callbacks if the async commit fails.
 * </p>
 * <p>
 * The MessageListener does not guarantee an exactly once or at most once
 * semantic. E.g. in case of rebalancing, some messages might be received
 * several times (eventually from different consumers)
 * </p>
 *
 *
 * @param <K>
 *           Class of the key of the read Kafka record
 * @param <V>
 *           Class of the value of the read Kafka record
 */
public class MessageListener<K, V> implements Runnable {

   private final long pollInterval;
   private final long topicMissingRetryMs;

   public enum CommitType {
      SYNC, ASYNC;
   }

   private static final Logger LOGGER = LoggerFactory.getLogger(MessageListener.class);

   private final MessageHandler<K, V> handler;
   private ErrorHandler<K, V> errorHandler;

   private final Collection<String> topics;

   private final AtomicBoolean shouldStop = new AtomicBoolean(false);

   private final KafkaConsumer<K, V> consumer;

   private final boolean autoCommitOnly;

   private final CommitType commitType;

   public MessageListener(MessageHandlerRegistration<K, V> registration, KafkaConsumer<K, V> consumer,
         ListenerConfig listenerConfig) {
      this.topics = registration.getTopicsNames();
      this.consumer = consumer;
      consumer.subscribe(topics);
      this.handler = registration.getHandler();
      this.errorHandler = registration.getErrorHandler();
      this.pollInterval = listenerConfig.getPollInterval();
      this.autoCommitOnly = listenerConfig.isUseAutoCommitOnly();
      this.commitType = listenerConfig.getCommitType();
      this.topicMissingRetryMs = listenerConfig.getTopicMissingRetryMs();
   }

   @Override
   public void run() {
      shouldStop.set(false);
      String joinedTopics = String.join(",", topics);
      waitForTopic(joinedTopics);

      while (!shouldStop.get()) {
         // return immediately and resubmit Runnable
         try {

            ConsumerRecords<K, V> records = consumer.poll(pollInterval);
            LOGGER.debug("Received {} messages from topics [{}]", records.count(), joinedTopics);

            processRecords(records);

            if (!autoCommitOnly && records.count() > 0) {
               commit();
            }

         } catch (WakeupException w) {
            LOGGER.warn("Woke up before polling returned", w);
         } catch (StopListenerException e) {
            LOGGER.error("Stopping listener for topics [{}] due to exception", joinedTopics, e);
            break;
         } catch (RuntimeException re) {
            LOGGER.error("Unauthorized or other runtime exception.", re);
         }
      }
      LOGGER.info("MessageListener closing Consumer for [{}]", joinedTopics);

      try {
         if (autoCommitOnly) {
            // try to commit explicitly since waiting for auto commit may be
            // lost due to closing the consumer
            try {
               consumer.commitSync();
            } catch (CommitFailedException e) {
               LOGGER.error("Commit failed", e);
            }
         }
      } finally {
         // close will auto-commit if enabled
         consumer.close();
      }
   }

   private void waitForTopic(String joinedTopics) {
      // Consumer waits until the topic is up, since the KafkaConsumer.poll call
      // floods log file with warnings
      // see https://issues.apache.org/jira/browse/KAFKA-4164
      if (topicMissingRetryMs > 0) {
         while (!shouldStop.get() && topics
               .stream()
               .map(consumer::partitionsFor)
               .flatMap(Collection::stream)
               .filter(Objects::nonNull)
               .collect(Collectors.toSet())
               .isEmpty()) {
            LOGGER.warn("Topics {} are not ready yet. Waiting {} ms for retry", joinedTopics, topicMissingRetryMs);
            try {
               Thread.sleep(topicMissingRetryMs);
            } catch (InterruptedException e) {
               LOGGER.error("Thread interrupted when waiting for topic to come up");
               Thread.currentThread().interrupt();
            }
         }
      }
   }

   /**
    * processes consumer records. Records are passed ony by one to the message
    * handler. If an exception occurs, error handler is invoked. If error
    * handler returns false, message processing is stopped completely
    * immediately
    * 
    * @param records
    *           records to process
    */
   private void processRecords(ConsumerRecords<K, V> records) {
      for (ConsumerRecord<K, V> record : records) {
         LOGGER.debug("Handling message for {}", record.key());
         try {
            handler.handle(record);
         } catch (RuntimeException e) {
            LOGGER.error("Error while handling record {} in message handler {}", record.key(), handler.getClass(), e);
            boolean shouldContinue = errorHandler.handleError(record, e, consumer);
            if (!shouldContinue) {
               throw new StopListenerException(e);
            }
         }
      }
   }

   private void commit() {
      if (commitType == CommitType.SYNC) {
         try {
            consumer.commitSync();
         } catch (CommitFailedException e) {
            LOGGER.error("Commit failed", e);
         }
      } else {
         if (!(handler instanceof CallbackMessageHandler)) {
            consumer.commitAsync();
         } else {
            consumer.commitAsync(this::handleOnComplete);
         }
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
