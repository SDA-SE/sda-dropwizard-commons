package org.sdase.commons.server.kafka.producer;

import java.util.concurrent.Future;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;

public class KafkaMessageProducer<K, V> implements MessageProducer<K, V> {

  private final String topic;

  private final Producer<K, V> producer;

  public KafkaMessageProducer(String topic, Producer<K, V> producer) {
    this.producer = producer;
    this.topic = topic;
  }

  @Override
  public Future<RecordMetadata> send(K key, V value, Headers headers, Callback callback) {
    ProducerRecord<K, V> producerRecord = new ProducerRecord<>(topic, null, key, value, headers);
    return producer.send(producerRecord, callback);
  }

  public void close() {
    producer.close();
  }

  @Override
  public void flush() {
    producer.flush();
  }
}
