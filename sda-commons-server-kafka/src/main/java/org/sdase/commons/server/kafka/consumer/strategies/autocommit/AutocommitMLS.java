package org.sdase.commons.server.kafka.consumer.strategies.autocommit;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.sdase.commons.server.kafka.consumer.ErrorHandler;
import org.sdase.commons.server.kafka.consumer.KafkaHelper;
import org.sdase.commons.server.kafka.consumer.MessageHandler;
import org.sdase.commons.server.kafka.consumer.StopListenerException;
import org.sdase.commons.server.kafka.consumer.strategies.MessageListenerStrategy;
import org.sdase.commons.server.kafka.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link MessageListenerStrategy} that uses autocommit only. */
public class AutocommitMLS<K, V> extends MessageListenerStrategy<K, V> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AutocommitMLS.class);
  private final MessageHandler<K, V> handler;
  private final ErrorHandler<K, V> errorHandler;
  private String consumerName;

  public AutocommitMLS(MessageHandler<K, V> handler, ErrorHandler<K, V> errorHandler) {
    this.handler = handler;
    this.errorHandler = errorHandler;
  }

  @Override
  public void processRecords(ConsumerRecords<K, V> records, KafkaConsumer<K, V> consumer) {
    if (consumerName == null) {
      consumerName = KafkaHelper.getClientId(consumer);
    }

    for (ConsumerRecord<K, V> consumerRecord : records) {
      LOGGER.debug("Handling message for {}", consumerRecord.key());
      try (var ignored = messageHandlerContextFor(consumerRecord)) {
        try {
          Instant timerStart = Instant.now();
          handler.handle(consumerRecord);
          addOffsetToCommitOnClose(consumerRecord);

          Instant timerEnd = Instant.now();
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                "calculated duration {} for message consumed by {} from {}",
                Duration.between(timerStart, timerEnd).toSeconds(),
                consumerName,
                consumerRecord.topic());
          }

        } catch (RuntimeException e) {
          LOGGER.error(
              "Error while handling record {} in message handler {}",
              consumerRecord.key(),
              handler.getClass(),
              e);
          boolean shouldContinue = errorHandler.handleError(consumerRecord, e, consumer);
          if (shouldContinue) {
            addOffsetToCommitOnClose(consumerRecord);
          } else {
            throw new StopListenerException(e);
          }
        }
      }
    }
  }

  @Override
  public void verifyConsumerConfig(Map<String, String> config) {
    if (Boolean.FALSE.equals(Boolean.valueOf(config.getOrDefault("enable.auto.commit", "true")))) {
      throw new ConfigurationException(
          "The strategy should use autocommit but property 'enable.auto.commit' in consumer config is set to 'false'");
    }
  }

  @Override
  public Map<String, String> forcedConfigToApply() {
    return Collections.singletonMap(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
  }

  @Override
  public void setRetryCounterIfApplicable(long maxRetriesCount) {
    // noop
    LOGGER.info("Retry counter is not applicable for autocommit strategy");
  }
}
