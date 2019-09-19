package org.sdase.commons.server.kafka.consumer.strategies.legacy;

import io.prometheus.client.SimpleTimer;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.CommitFailedException;
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
 * @deprecated Strategy that supports the legacy implementation used before the introduction of
 *     {@link MessageListenerStrategy}.
 *     <p>The strategy is configurable but not very flexible. The strategy pattern should provide
 *     more flexibility to the developer to influence the behavior of Kafka communication.
 */
@Deprecated
public class LegacyMLS<K, V> extends MessageListenerStrategy<K, V> {

  private static final Logger LOGGER = LoggerFactory.getLogger(LegacyMLS.class);

  private final MessageHandler<K, V> handler;
  private final boolean autoCommitOnly;
  private final CommitType commitType;
  private ErrorHandler<K, V> errorHandler;
  private String consumerName;

  public LegacyMLS(
      MessageHandler<K, V> handler,
      ErrorHandler<K, V> errorHandler,
      boolean autoCommitOnly,
      CommitType commitType) {
    this.handler = handler;
    this.errorHandler = errorHandler;
    this.autoCommitOnly = autoCommitOnly;
    this.commitType = commitType;
  }

  @Override
  public void processRecords(ConsumerRecords<K, V> records, KafkaConsumer<K, V> consumer) {
    if (consumerName == null) {
      consumerName = KafkaHelper.getClientId(consumer);
    }
    for (ConsumerRecord<K, V> record : records) {
      LOGGER.debug("Handling message for {}", record.key());
      try {
        SimpleTimer timer = new SimpleTimer();
        handler.handle(record);

        // Prometheus
        double elapsedSeconds = timer.elapsedSeconds();
        consumerProcessedMsgHistogram.observe(elapsedSeconds, consumerName, record.topic());

        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace(
              "calculated duration {} for message consumed by {} from {}",
              elapsedSeconds,
              consumerName,
              record.topic());
        }
      } catch (RuntimeException e) {
        LOGGER.error(
            "Error while handling record {} in message handler {}",
            record.key(),
            handler.getClass(),
            e);
        boolean shouldContinue = errorHandler.handleError(record, e, consumer);
        if (!shouldContinue) {
          throw new StopListenerException(e);
        }
      }
    }
    if (!autoCommitOnly && records.count() > 0) {
      commit(consumer);
    }
  }

  @Override
  public void commitOnClose(KafkaConsumer<K, V> consumer) {
    if (autoCommitOnly) {
      // try to commit explicitly since waiting for auto commit may be
      // lost due to closing the consumer
      try {
        consumer.commitSync();
      } catch (CommitFailedException e) {
        LOGGER.error("Commit failed", e);
      }
    }
  }

  @Override
  public void verifyConsumerConfig(Map<String, String> config) {
    if (autoCommitOnly && !Boolean.valueOf(config.getOrDefault("enable.auto.commit", "true"))) {
      throw new ConfigurationException(
          "The strategy should use autocommit but property 'enable.auto.commit' in consumer config is set to 'false'");
    }
  }

  private void commit(KafkaConsumer consumer) {
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

  private void handleOnComplete(
      Map<TopicPartition, OffsetAndMetadata> offsets, Exception exception) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "Handling callback for topic partition {}",
          offsets.keySet().stream()
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

  public enum CommitType {
    SYNC,
    ASYNC;
  }
}
