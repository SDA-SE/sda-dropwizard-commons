package org.sdase.commons.server.kafka.consumer.strategies.retryprocessingerror;

import io.prometheus.client.SimpleTimer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.sdase.commons.server.kafka.consumer.KafkaHelper;
import org.sdase.commons.server.kafka.consumer.ErrorHandler;
import org.sdase.commons.server.kafka.consumer.MessageHandler;
import org.sdase.commons.server.kafka.consumer.StopListenerException;
import org.sdase.commons.server.kafka.consumer.strategies.MessageListenerStrategy;
import org.sdase.commons.server.kafka.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link MessageListenerStrategy} commits records for each partition.
 * In case of processing errors an error handler can decide if processing should be retried or stopped.
 */
public class RetryProcessingErrorMLS<K, V> extends MessageListenerStrategy<K, V> {

   private static final Logger LOGGER = LoggerFactory.getLogger(RetryProcessingErrorMLS.class);
   private final MessageHandler<K, V> handler;
   private final ErrorHandler<K, V> errorHandler;
   private String consumerName;

   public RetryProcessingErrorMLS(MessageHandler<K, V> handler, ErrorHandler<K, V> errorHandler) {
      this.handler = handler;
      this.errorHandler = errorHandler;
   }

   @Override
   public void processRecords(ConsumerRecords<K, V> records, KafkaConsumer<K, V> consumer) {
      if (consumerName == null) {
         consumerName = KafkaHelper.getClientId(consumer);
      }

      for (TopicPartition partition : records.partitions()) {
         processRecordsByPartition(records, consumer, partition);
      }
   }

   private void processRecordsByPartition(ConsumerRecords<K, V> records,
       KafkaConsumer<K, V> consumer, TopicPartition partition) {
      List<ConsumerRecord<K, V>> partitionRecords = records.records(partition);
      OffsetAndMetadata lastCommitOffset = null;
      for (ConsumerRecord<K, V> record : partitionRecords) {
         LOGGER.debug("Handling message for {}", record.key());

         try {
            SimpleTimer timer = new SimpleTimer();
            handler.handle(record);
            // mark last successful processed record for commit
            lastCommitOffset = new OffsetAndMetadata(record.offset()+1);

            // Prometheus
            double elapsedSeconds = timer.elapsedSeconds();
            consumerProcessedMsgHistogram.observe(elapsedSeconds, consumerName, record.topic());

            if (LOGGER.isTraceEnabled()) {
               LOGGER
                   .trace("calculated duration {} for message consumed by {} from {}",
                       elapsedSeconds, consumerName,
                       record.topic());
            }

         } catch (RuntimeException e) {
            LOGGER.error("Error while handling record {} in message handler {}", record.key(),
                handler.getClass(), e);
            boolean shouldContinue = errorHandler.handleError(record, e, consumer);
            if (!shouldContinue) {
               throw new StopListenerException(e);
            } else {
               LOGGER.warn("Error while handling record {} in message handler {}, will be retried", record.key(),
                   handler.getClass(), e);

               // seek to the current offset of the failing record for retry
               consumer.seek(partition, record.offset());
               break;
            }
         }
      }
      if (lastCommitOffset != null) {
         consumer.commitSync(Collections.singletonMap(partition, lastCommitOffset));
      }
   }

   @Override
   public void commitOnClose(KafkaConsumer<K, V> consumer) {
      try {
         consumer.commitSync();
      } catch (CommitFailedException e) {
         LOGGER.error("Commit failed", e);
      }
   }

   @Override
   public void verifyConsumerConfig(Map<String, String> config) {
      if (Boolean.valueOf(config.getOrDefault("enable.auto.commit", "true"))) {
         throw new ConfigurationException(
               "The strategy should commit explicitly by partition but property 'enable.auto.commit' in consumer config is set to 'true'");
      }
   }

}
