package org.sdase.commons.server.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.producer.Producer;

/**
 * Listener adds or removes Kafka internal metrics for each producer to a micrometer registry. Made
 * package private to not expose micrometer
 */
class MicrometerProducerListener {

  private final MeterRegistry meterRegistry;

  private final Map<String, KafkaClientMetrics> metrics = new HashMap<>();

  MicrometerProducerListener(MeterRegistry meterRegistry) {
    this(meterRegistry, Collections.emptyList());
  }

  MicrometerProducerListener(MeterRegistry meterRegistry, List<Tag> tags) {
    this.meterRegistry = meterRegistry;
    this.tags = tags;
  }

  private final List<Tag> tags;

  /**
   * Binds Kafka internal metrics to a micrometer registry.
   *
   * @param clientId producer id
   * @param producer Kafka internal producer
   */
  synchronized <K, V> void producerAdded(String clientId, Producer<K, V> producer) {

    this.metrics.computeIfAbsent(
        clientId,
        k -> {
          List<Tag> producerTags = new ArrayList<>(this.tags);
          KafkaClientMetrics kafkaClientMetrics = new KafkaClientMetrics(producer, producerTags);
          kafkaClientMetrics.bindTo(this.meterRegistry);
          return kafkaClientMetrics;
        });
  }

  /**
   * Removes Kafka internal metrics from a micrometer registry.
   *
   * @param clientId producer id
   */
  synchronized void producerRemoved(String clientId) {
    KafkaClientMetrics removed = this.metrics.remove(clientId);
    if (removed != null) {
      removed.close();
    }
  }
}
