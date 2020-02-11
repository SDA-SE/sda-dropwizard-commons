package org.sdase.commons.server.kafka.producer;

import java.io.Closeable;
import java.util.concurrent.Future;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;

/**
 * A Kafka client that publishes records to the Kafka cluster for a specific topic.
 *
 * @param <K> key class to send
 * @param <V> value class to send
 */
public interface MessageProducer<K, V> extends Closeable {

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

  /** closes this producer */
  void close();
}
