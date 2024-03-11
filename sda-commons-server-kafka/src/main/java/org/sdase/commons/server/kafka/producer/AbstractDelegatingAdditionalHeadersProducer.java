package org.sdase.commons.server.kafka.producer;

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
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;

/**
 * A {@link Producer} that adds additional headers to the {@link ProducerRecord}s it sends. The
 * headers are provided by the actual implementation. All methods declared in {@link Producer}
 * delegate to the given {@link Producer}.
 *
 * @param <K> the type of the message key
 * @param <V> the type of the message value
 */
public abstract class AbstractDelegatingAdditionalHeadersProducer<K, V> implements Producer<K, V> {

  private final Producer<K, V> delegate;

  protected AbstractDelegatingAdditionalHeadersProducer(Producer<K, V> delegate) {
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
    return delegate.send(withAdditionalHeaders(producerRecord));
  }

  @Override
  public Future<RecordMetadata> send(ProducerRecord<K, V> producerRecord, Callback callback) {
    return delegate.send(withAdditionalHeaders(producerRecord), callback);
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

  private ProducerRecord<K, V> withAdditionalHeaders(ProducerRecord<K, V> producerRecord) {
    var additionalHeaders = additionalHeaders(producerRecord);
    if (additionalHeaders == null) {
      return producerRecord;
    }

    var headers = new RecordHeaders(producerRecord.headers());
    additionalHeaders.forEach(headers::add);

    return new ProducerRecord<>(
        producerRecord.topic(),
        producerRecord.partition(),
        producerRecord.timestamp(),
        producerRecord.key(),
        producerRecord.value(),
        headers);
  }

  /**
   * Creates additional headers for the given {@code producerRecord}.
   *
   * @param producerRecord the {@link ProducerRecord} that may be sent with additional headers.
   * @return additional headers, may return {@code null}
   */
  protected abstract Headers additionalHeaders(ProducerRecord<K, V> producerRecord);
}
