package org.sdase.commons.server.kafka.consumer.strategies.retryprocessingerror;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.sdase.commons.server.kafka.consumer.ErrorHandler;
import org.sdase.commons.server.kafka.consumer.KafkaHelper;
import org.sdase.commons.server.kafka.consumer.MessageHandler;
import org.sdase.commons.server.kafka.consumer.StopListenerException;
import org.sdase.commons.server.kafka.consumer.strategies.MessageListenerStrategy;
import org.sdase.commons.server.kafka.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link MessageListenerStrategy} commits records for each partition. In case of processing errors
 * an error handler can decide if processing should be retried or stopped.
 */
public class RetryProcessingErrorMLS<K, V> extends MessageListenerStrategy<K, V> {

  private static final Logger LOGGER = LoggerFactory.getLogger(RetryProcessingErrorMLS.class);
  private final MessageHandler<K, V> handler;
  private final ErrorHandler<K, V> intermediateErrorHandler;
  private final ErrorHandler<K, V> finalErrorHandler;
  private final long maxRetryCount;
  private String consumerName;
  private Map<TopicPartition, OffsetCounter> offsetCounters =
      Collections.synchronizedMap(new HashMap<>());

  public RetryProcessingErrorMLS(
      MessageHandler<K, V> handler, ErrorHandler<K, V> intermediateErrorHandler) {
    this(handler, intermediateErrorHandler, Long.MAX_VALUE, null);
  }

  public RetryProcessingErrorMLS(
      MessageHandler<K, V> handler,
      ErrorHandler<K, V> intermediateErrorHandler,
      long maxRetryCount,
      ErrorHandler<K, V> finalErrorHandler) {
    this.handler = handler;
    this.intermediateErrorHandler = intermediateErrorHandler;
    this.maxRetryCount = maxRetryCount;
    this.finalErrorHandler = finalErrorHandler;
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

  private void processRecordsByPartition(
      ConsumerRecords<K, V> records, KafkaConsumer<K, V> consumer, TopicPartition partition) {
    List<ConsumerRecord<K, V>> partitionRecords = records.records(partition);
    OffsetAndMetadata lastCommitOffset = null;
    for (ConsumerRecord<K, V> consumerRecord : partitionRecords) {
      LOGGER.debug("Handling message for {}", consumerRecord.key());

      try (var ignored = messageHandlerContextFor(consumerRecord)) {
        try {
          Instant timerStart = Instant.now();
          handler.handle(consumerRecord);
          lastCommitOffset = markConsumerRecordProcessed(consumerRecord);

          Instant timerEnd = Instant.now();
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                "calculated duration {} for message consumed by {} from {}",
                Duration.between(timerStart, timerEnd).toSeconds(),
                consumerName,
                consumerRecord.topic());
          }

        } catch (RuntimeException e) {
          long retryCount = getOffsetCounter(partition, consumerRecord.offset()).inc();
          if (retryCount > maxRetryCount) {
            LOGGER.error(
                "Error while handling record {} in message handler {}, no more retries",
                consumerRecord.key(),
                handler.getClass(),
                e);

            callErrorHandler(finalErrorHandler, consumerRecord, e, consumer);
            lastCommitOffset = markConsumerRecordProcessed(consumerRecord);
          } else {
            LOGGER.warn(
                "Error while handling record {} in message handler {}, will be retried (%s / %s)..."
                    .formatted(retryCount, maxRetryCount),
                consumerRecord.key(),
                handler.getClass(),
                e);

            callErrorHandler(intermediateErrorHandler, consumerRecord, e, consumer);

            // seek to the current offset of the failing record for retry
            consumer.seek(partition, consumerRecord.offset());
            break;
          }
        }
      }
    }
    if (lastCommitOffset != null) {
      consumer.commitSync(Collections.singletonMap(partition, lastCommitOffset));
    }
  }

  @Override
  public void verifyConsumerConfig(Map<String, String> config) {
    if (Boolean.TRUE.equals(Boolean.valueOf(config.getOrDefault("enable.auto.commit", "true")))) {
      throw new ConfigurationException(
          "The strategy should commit explicitly by partition but property 'enable.auto.commit' in consumer config is set to 'true'");
    }
  }

  private void callErrorHandler(
      ErrorHandler<K, V> errorHandler,
      ConsumerRecord<K, V> consumerRecord,
      RuntimeException e,
      KafkaConsumer<K, V> consumer) {
    if (errorHandler != null) {
      boolean shouldContinue = errorHandler.handleError(consumerRecord, e, consumer);
      if (!shouldContinue) {
        throw new StopListenerException(e);
      }
    }
  }

  private OffsetAndMetadata markConsumerRecordProcessed(ConsumerRecord<K, V> consumerRecord) {
    addOffsetToCommitOnClose(consumerRecord);
    return new OffsetAndMetadata(consumerRecord.offset() + 1);
  }

  private OffsetCounter getOffsetCounter(TopicPartition partition, long offset) {
    OffsetCounter counter = offsetCounters.computeIfAbsent(partition, k -> new OffsetCounter(0));
    if (counter.offset != offset) {
      counter.offset = offset;
      counter.count = 0;
    }

    return counter;
  }

  public static class OffsetCounter {
    private long offset = 0;
    private long count = 0;

    public OffsetCounter(long offset) {
      this.offset = offset;
      this.count = 0;
    }

    public long inc() {
      return ++count;
    }
  }
}
