package org.sdase.commons.server.kafka.consumer.strategies.synccommit;

import org.apache.kafka.clients.consumer.Consumer;

/**
 * Interface for handling errors that might occur when committing the offset. Be aware that the key
 * or value can be null if the WrappedNoSerializationErrorDeserializer is used.
 *
 * @param <K> key class of the record
 * @param <V> value class of the record
 */
@FunctionalInterface
public interface SyncCommitErrorHandler<K, V> {

  /**
   * Handling error that happened during committing.
   *
   * @param exception error that occurred
   * @param consumer consumer that read the record
   */
  void handleError(RuntimeException exception, Consumer<K, V> consumer);
}
