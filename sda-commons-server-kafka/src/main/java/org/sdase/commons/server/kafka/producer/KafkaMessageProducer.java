package org.sdase.commons.server.kafka.producer;

import java.util.concurrent.Future;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.sdase.commons.server.kafka.prometheus.ProducerTopicMessageCounter;
import org.sdase.commons.shared.tracing.RequestTracing;
import org.sdase.commons.shared.tracing.TraceContext;

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
    ProducerRecord<K, V> producerRecord = new ProducerRecord<>(topic, key, value);
    addTraceTokenToHeader(producerRecord);
    msgCounter.increase(producerName, producerRecord.topic());
    return producer.send(producerRecord);
  }

  @Override
  public Future<RecordMetadata> send(K key, V value, Headers headers) {
    ProducerRecord<K, V> producerRecord = new ProducerRecord<>(topic, null, key, value, headers);
    addTraceTokenToHeader(producerRecord);
    msgCounter.increase(producerName, producerRecord.topic());
    return producer.send(producerRecord);
  }

  private void addTraceTokenToHeader(ProducerRecord<K, V> producerRecord) {
    String traceToken = TraceContext.getCurrentOrCreateNewTraceToken();
    if (traceToken != null) {
      producerRecord
          .headers()
          .add(new RecordHeader(RequestTracing.TOKEN_HEADER, traceToken.getBytes()));
    }
  }

  public void close() {
    producer.close();
  }

  @Override
  public void flush() {
    producer.flush();
  }
}
