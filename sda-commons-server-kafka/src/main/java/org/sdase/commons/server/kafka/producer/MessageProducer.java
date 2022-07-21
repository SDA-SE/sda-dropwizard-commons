package org.sdase.commons.server.kafka.producer;

import java.util.concurrent.Future;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;

/**
 * A Kafka client that publishes records to the Kafka cluster for a specific topic.
 *
 * @param <K> key class to send
 * @param <V> value class to send
 */
public interface MessageProducer<K, V> {
  /**
   * Asynchronously send a record to a specific topic
   *
   * @param key key to send
   * @param value value to send
   * @return The result of the send is a {@link RecordMetadata} specifying the partition the record
   *     was sent to, the offset it was assigned and the timestamp of the record.
   */
  Future<RecordMetadata> send(K key, V value);

  Future<RecordMetadata> send(K key, V value, Headers headers);

  /**
   * This method is a blank default implementation in order to avoid it being a breaking change. The
   * implementing class must override this to add behaviour to it. The implementation should call
   * the flush() method of the producer.
   */
  default void flush() {}
}
