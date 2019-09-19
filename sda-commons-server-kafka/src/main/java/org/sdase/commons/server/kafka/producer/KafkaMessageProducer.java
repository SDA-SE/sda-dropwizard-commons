package org.sdase.commons.server.kafka.producer;

import java.util.concurrent.Future;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.sdase.commons.server.kafka.prometheus.ProducerTopicMessageCounter;

public class KafkaMessageProducer<K, V> implements MessageProducer<K, V> {

  private String topic;

  private KafkaProducer<K, V> producer;

  private ProducerTopicMessageCounter msgCounter;

  private String producerName;

  public KafkaMessageProducer(
      String topic,
      KafkaProducer<K, V> producer,
      ProducerTopicMessageCounter msgCounter,
      String producerName) {
    this.producer = producer;
    this.topic = topic;
    this.msgCounter = msgCounter;
    this.producerName = producerName;
  }

  @Override
  public Future<RecordMetadata> send(K key, V value) {
    ProducerRecord<K, V> record = new ProducerRecord<>(topic, key, value);
    msgCounter.increase(producerName, record.topic());
    return producer.send(record);
  }

  @Override
  public Future<RecordMetadata> send(K key, V value, Headers headers) {
    ProducerRecord<K, V> record = new ProducerRecord<>(topic, null, key, value, headers);
    msgCounter.increase(producerName, record.topic());
    return producer.send(record);
  }

  @Override
   public void close() {
      producer.close();
   }
}
