package org.sdase.commons.server.kafka.producer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.errors.ProducerFencedException;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.sdase.commons.shared.tracing.TraceTokenContext;

public class TraceTokenAwareKafkaProducer<K, V> implements Producer<K, V> {

  private final Producer<K, V> delegate;

  public TraceTokenAwareKafkaProducer(Producer<K, V> delegate) {
    this.delegate = delegate;
  }

  @Override
  public void initTransactions() {
    delegate.initTransactions();
  }

  @Override
  public void beginTransaction() throws ProducerFencedException {
    delegate.beginTransaction();
  }

  @Override
  @SuppressWarnings("deprecation")
  public void sendOffsetsToTransaction(
      Map<TopicPartition, OffsetAndMetadata> offsets, String consumerGroupId)
      throws ProducerFencedException {
    delegate.sendOffsetsToTransaction(offsets, consumerGroupId);
  }

  @Override
  public void sendOffsetsToTransaction(
      Map<TopicPartition, OffsetAndMetadata> offsets, ConsumerGroupMetadata groupMetadata)
      throws ProducerFencedException {
    delegate.sendOffsetsToTransaction(offsets, groupMetadata);
  }

  @Override
  public void commitTransaction() throws ProducerFencedException {
    delegate.commitTransaction();
  }

  @Override
  public void abortTransaction() throws ProducerFencedException {
    delegate.abortTransaction();
  }

  @Override
  public Future<RecordMetadata> send(ProducerRecord<K, V> producerRecord) {
    return delegate.send(withTraceToken(producerRecord));
  }

  @Override
  public Future<RecordMetadata> send(ProducerRecord<K, V> producerRecord, Callback callback) {
    return delegate.send(withTraceToken(producerRecord), callback);
  }

  @Override
  public void flush() {
    delegate.flush();
  }

  @Override
  public List<PartitionInfo> partitionsFor(String topic) {
    return delegate.partitionsFor(topic);
  }

  @Override
  public Map<MetricName, ? extends Metric> metrics() {
    return delegate.metrics();
  }

  @Override
  public Uuid clientInstanceId(Duration timeout) {
    return delegate.clientInstanceId(timeout);
  }

  @Override
  public void close() {
    delegate.close();
  }

  @Override
  public void close(Duration timeout) {
    delegate.close(timeout);
  }

  private ProducerRecord<K, V> withTraceToken(ProducerRecord<K, V> producerRecord) {
    if (!TraceTokenContext.isTraceTokenContextActive()) {
      return producerRecord;
    }

    try (var traceTokenContext = TraceTokenContext.getOrCreateTraceTokenContext()) {
      var headers = new RecordHeaders(producerRecord.headers());
      headers.add(
          TraceTokenContext.TRACE_TOKEN_MESSAGING_HEADER_NAME,
          traceTokenContext.get().getBytes(StandardCharsets.UTF_8));

      return new ProducerRecord<>(
          producerRecord.topic(),
          producerRecord.partition(),
          producerRecord.timestamp(),
          producerRecord.key(),
          producerRecord.value(),
          headers);
    }
  }
}
