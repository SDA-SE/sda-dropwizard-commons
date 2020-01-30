package org.sdase.commons.server.kafka.consumer.strategies.synccommit;

import io.prometheus.client.SimpleTimer;
import java.util.Collections;
import java.util.Map;
import org.apache.kafka.clients.consumer.CommitFailedException;
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

/** {@link MessageListenerStrategy} that uses sync commit explicitly before polling a new chunk */
public class SyncCommitMLS<K, V> extends MessageListenerStrategy<K, V> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SyncCommitMLS.class);
  private final MessageHandler<K, V> handler;
  private final ErrorHandler<K, V> errorHandler;
  private String consumerName;

  public SyncCommitMLS(MessageHandler<K, V> handler, ErrorHandler<K, V> errorHandler) {
    this.handler = handler;
    this.errorHandler = errorHandler;
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
    commit(consumer);
  }

  @Override
  public void commitOnClose(KafkaConsumer<K, V> consumer) {
    commit(consumer);
  }

  private void commit(KafkaConsumer<K, V> consumer) {
    try {
      consumer.commitSync();
    } catch (CommitFailedException e) {
      LOGGER.error("Commit failed", e);
    }
  }

  @Override
  public void verifyConsumerConfig(Map<String, String> config) {
    if (Boolean.valueOf(config.getOrDefault(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true"))) {
      throw new ConfigurationException(
          "The strategy should NOT use autocommit but property 'enable.auto.commit' in consumer config is set to 'true' (which is the default and must be disabled).");
    }
  }

  @Override
  public Map<String, String> forcedConfigToApply() {
    return Collections.singletonMap(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
  }
}
