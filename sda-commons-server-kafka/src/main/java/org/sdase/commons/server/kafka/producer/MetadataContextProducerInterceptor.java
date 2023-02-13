package org.sdase.commons.server.kafka.producer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.sdase.commons.server.dropwizard.metadata.MetadataContext;

public class MetadataContextProducerInterceptor<K, V> implements ProducerInterceptor<K, V> {

  private final Set<String> metadataFields;

  public MetadataContextProducerInterceptor() {
    this.metadataFields = MetadataContext.metadataFields();
  }

  @Override
  public ProducerRecord<K, V> onSend(ProducerRecord<K, V> producerRecord) {
    if (metadataFields.isEmpty()) {
      return producerRecord;
    }
    var headers = producerRecord.headers();
    var metadataContext = MetadataContext.current();
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

    return producerRecord;
  }

  @Override
  public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
    // nothing to do here
  }

  @Override
  public void close() {
    // nothing to do here
  }

  @Override
  public void configure(Map<String, ?> configs) {
    // nothing to do here
  }
}
