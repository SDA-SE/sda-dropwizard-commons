package org.sdase.commons.server.kafka.consumer.strategies.retryprocessingerror;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
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
 * the message will be retried for a configured amount of times.
 *
 * <p>After each error an error handler can decide if processing should be retried or stopped.
 *
 * <p>If the retry count exceeds:
 *
 * <ul>
 *   <li>The failed message handling will be logged as ERROR
 *   <li>a retryLimitExceededErrorHandler will be called allowing to do more error handling (i.e.
 *       write to DLT)
 *   <li>the offset will get increased, so the message handling gets ignored.
 * </ul>
 */
public class RetryProcessingErrorMLS<K, V> extends MessageListenerStrategy<K, V> {

  private static final Logger LOGGER = LoggerFactory.getLogger(RetryProcessingErrorMLS.class);
  private final MessageHandler<K, V> handler;
  private final ErrorHandler<K, V> errorHandler;
  private final ErrorHandler<K, V> retryLimitExceededErrorHandler;
  private String consumerName;
  private final RetryCounter retryCounter;

  /**
   * Creates a new instance of {@link RetryProcessingErrorMLS} retrying the message on error
   * infinite times.
   *
   * @param handler the message handler
   * @param errorHandler the error handler called after each error
   */
  public RetryProcessingErrorMLS(MessageHandler<K, V> handler, ErrorHandler<K, V> errorHandler) {
    this(handler, errorHandler, Long.MAX_VALUE, null);
  }

  /**
   * Creates a new instance of {@link RetryProcessingErrorMLS} retrying the message on error for a
   * given amount of times
   *
   * @param handler the message handler
   * @param errorHandler the error handler called after each error
   * @param maxRetryCount the maximum number of retries
   * @param retryLimitExceededErrorHandler the error handler called if the retry limit is exceeded
   */
  public RetryProcessingErrorMLS(
      MessageHandler<K, V> handler,
      ErrorHandler<K, V> errorHandler,
      long maxRetryCount,
      ErrorHandler<K, V> retryLimitExceededErrorHandler) {
    this.handler = handler;
    this.errorHandler = errorHandler;
    this.retryLimitExceededErrorHandler = retryLimitExceededErrorHandler;
    this.retryCounter = new RetryCounter(maxRetryCount);
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
          retryCounter.incErrorCount(consumerRecord);
          if (retryCounter.isMaxRetryCountReached(consumerRecord)) {
            LOGGER.error(
                "Error while handling record {} in message handler {}, no more retries",
                consumerRecord.key(),
                handler.getClass(),
                e);

            callErrorHandler(retryLimitExceededErrorHandler, consumerRecord, e, consumer);
            lastCommitOffset = markConsumerRecordProcessed(consumerRecord);
          } else {
            LOGGER.warn(
                "Error while handling record {} in message handler {}, will be retried ({} / {})...",
                consumerRecord.key(),
                handler.getClass(),
                retryCounter.getOffsetCounter(consumerRecord),
                retryCounter.getMaxRetryCount(),
                e);

            callErrorHandler(errorHandler, consumerRecord, e, consumer);

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
}
