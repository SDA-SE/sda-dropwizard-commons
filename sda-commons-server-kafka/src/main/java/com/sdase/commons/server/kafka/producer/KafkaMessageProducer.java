package com.sdase.commons.server.kafka.producer;

import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;

public class KafkaMessageProducer<K, V> implements MessageProducer<K, V> {

   private String topic;

   private KafkaProducer<K, V> producer;

   public KafkaMessageProducer(String topic, KafkaProducer<K, V> producer) {
      this.producer = producer;
      this.topic = topic;
   }

   @Override
   public Future<RecordMetadata> send(K key, V value) {
      ProducerRecord<K, V> record = new ProducerRecord<>(topic, key, value);
      return producer.send(record);
   }

   @Override
   public Future<RecordMetadata> send(K key, V value, Headers headers) {
      ProducerRecord<K, V> record = new ProducerRecord<>(topic, null, key, value, headers);
      return producer.send(record);
   }

   public void close() {
      producer.close();
   }

}
