package org.sdase.commons.server.kafka.producer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
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
public class MetadataContextAwareKafkaProducer<K, V>
    extends AbstractDelegatingAdditionalHeadersProducer<K, V> {

  private final Set<String> metadataFields;

  /**
   * @param delegate the actual {@link Producer} that is used to interact with the Kafka cluster.
   * @param metadataFields the configured fields that are used as metadata
   */
  public MetadataContextAwareKafkaProducer(Producer<K, V> delegate, Set<String> metadataFields) {
    super(delegate);
    this.metadataFields = Optional.ofNullable(metadataFields).orElse(Set.of());
  }

  @Override
  protected Headers additionalHeaders(ProducerRecord<K, V> producerRecord) {
    if (metadataFields.isEmpty()) {
      return null;
    }

    Headers headers = new RecordHeaders();

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
    return headers;
  }
}
