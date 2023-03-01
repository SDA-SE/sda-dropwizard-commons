package org.sdase.commons.server.kafka.producer;

import java.util.concurrent.Future;
import java.util.function.BiFunction;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.sdase.commons.server.kafka.prometheus.ProducerTopicMessageCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaMessageProducer<K, V> implements MessageProducer<K, V> {

  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMessageProducer.class);

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
    return producer.send(record, callbackProxy.apply(key, null));
  }

  @Override
  public Future<RecordMetadata> send(K key, V value, Headers headers) {
    ProducerRecord<K, V> record = new ProducerRecord<>(topic, null, key, value, headers);
    msgCounter.increase(producerName, record.topic());
    return producer.send(record, callbackProxy.apply(key, null));
  }

  @Override
  public Future<RecordMetadata> send(K key, V value, Headers headers, Callback callback) {
    ProducerRecord<K, V> record = new ProducerRecord<>(topic, null, key, value, headers);
    msgCounter.increase(producerName, record.topic());
    return producer.send(record, callbackProxy.apply(key, callback));
  }

  public void close() {
    producer.close();
  }

  @Override
  public void flush() {
    producer.flush();
  }

  private final BiFunction<K, Callback, Callback> callbackProxy =
      (messageKey, callback) ->
          (metadata, exception) -> {
            if (exception != null) {
              LOGGER.error(
                  "An error occurred while producing a message with key {} to the topic {}.",
                  messageKey,
                  topic,
                  exception);
            } else {
              // avoid noise
              LOGGER.debug(
                  "Message with key {} was produced to topic {}: partition {}; offset: {}.",
                  messageKey,
                  topic,
                  metadata.partition(),
                  metadata.offset());
            }

            if (callback == null) {
              return;
            }
            // delegate
            callback.onCompletion(metadata, exception);
          };
}
