package org.sdase.commons.server.kafka.consumer.strategies.synccommit;

import java.util.Collections;
import java.util.Map;
import org.apache.kafka.clients.consumer.*;
import org.sdase.commons.server.kafka.consumer.ErrorHandler;
import org.sdase.commons.server.kafka.consumer.MessageHandler;
import org.sdase.commons.server.kafka.consumer.strategies.MessageListenerStrategy;
import org.sdase.commons.server.kafka.consumer.strategies.autocommit.AutocommitMLS;
import org.sdase.commons.server.kafka.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link MessageListenerStrategy} that uses sync commit explicitly before polling a new chunk */
public class SyncCommitMLS<K, V> extends AutocommitMLS<K, V> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SyncCommitMLS.class);

  private final SyncCommitErrorHandler<K, V> syncCommitErrorHandler;

  public SyncCommitMLS(MessageHandler<K, V> handler, ErrorHandler<K, V> errorHandler) {
    this(
        handler,
        errorHandler,
        (RuntimeException e, Consumer<K, V> consumer) -> {
          if (!(e instanceof CommitFailedException)) {
            /*
             rethrow everything that is not a CommitFailedException to keep the original behaviour
             if no custom syncCommitErrorHandler is used
            */

            throw e;
          }
        });
  }

  public SyncCommitMLS(
      MessageHandler<K, V> handler,
      ErrorHandler<K, V> errorHandler,
      SyncCommitErrorHandler<K, V> syncCommitErrorHandler) {
    super(handler, errorHandler);
    this.syncCommitErrorHandler = syncCommitErrorHandler;
  }

  @Override
  public void processRecords(ConsumerRecords<K, V> records, KafkaConsumer<K, V> consumer) {
    // Only process and commit if there are records to process
    if (!records.isEmpty()) {
      super.processRecords(records, consumer);
      this.commitSync(consumer);
    }
  }

  private void commitSync(KafkaConsumer<K, V> consumer) {
    try {
      consumer.commitSync();
    } catch (RuntimeException exception) {
      LOGGER.error("Commit failed", exception);
      syncCommitErrorHandler.handleError(exception, consumer);
    }
  }

  @Override
  public void verifyConsumerConfig(Map<String, String> config) {
    if (Boolean.TRUE.equals(
        Boolean.valueOf(config.getOrDefault(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")))) {
      throw new ConfigurationException(
          "The strategy should NOT use autocommit but property 'enable.auto.commit' in consumer config is set to 'true' (which is the default and must be disabled).");
    }
  }

  @Override
  public Map<String, String> forcedConfigToApply() {
    return Collections.singletonMap(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
  }
}
