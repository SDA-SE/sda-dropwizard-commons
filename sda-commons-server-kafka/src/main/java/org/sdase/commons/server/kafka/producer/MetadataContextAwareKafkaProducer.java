package org.sdase.commons.server.kafka.producer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import org.apache.commons.lang3.StringUtils;
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
import org.sdase.commons.server.dropwizard.metadata.MetadataContext;

/**
 * A {@link Producer} that delegates all implementations to a delegate. {@link
 * ProducerRecord#headers()} are extended with information of the {@linkplain
 * MetadataContext#current() current} {@link
 * org.sdase.commons.server.dropwizard.metadata.MetadataContext}.
 *
 * @param <K> the type of the message key
 * @param <V> the type of the message value
 */
public class MetadataContextAwareKafkaProducer<K, V> implements Producer<K, V> {

  private final Producer<K, V> delegate;

  private final Set<String> metadataFields;

  /**
   * @param delegate the actual {@link Producer} that is used to interact with the Kafka cluster.
   * @param metadataFields the configured fields that are used as metadata
   */
  public MetadataContextAwareKafkaProducer(Producer<K, V> delegate, Set<String> metadataFields) {
    this.delegate = delegate;
    this.metadataFields = Optional.ofNullable(metadataFields).orElse(Set.of());
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
    return delegate.send(withMetadataContext(producerRecord));
  }

  @Override
  public Future<RecordMetadata> send(ProducerRecord<K, V> producerRecord, Callback callback) {
    return delegate.send(withMetadataContext(producerRecord), callback);
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

  private ProducerRecord<K, V> withMetadataContext(ProducerRecord<K, V> producerRecord) {
    if (metadataFields.isEmpty()) {
      return producerRecord;
    }
    var headers = new RecordHeaders(producerRecord.headers());
    MetadataContext metadataContext = MetadataContext.current();
    for (String metadataField : metadataFields) {
      List<String> valuesByKey = metadataContext.valuesByKey(metadataField);
      if (valuesByKey == null) {
        continue;
      }
      valuesByKey.stream()
          .filter(StringUtils::isNotBlank)
          .map(String::trim)
          .distinct()
          .map(v -> v.getBytes(StandardCharsets.UTF_8))
          .forEach(v -> headers.add(metadataField, v));
    }
    return new ProducerRecord<>(
        producerRecord.topic(),
        producerRecord.partition(),
        producerRecord.timestamp(),
        producerRecord.key(),
        producerRecord.value(),
        headers);
  }
}
