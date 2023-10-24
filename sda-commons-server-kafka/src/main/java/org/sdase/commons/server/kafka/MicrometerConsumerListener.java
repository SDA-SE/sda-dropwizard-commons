package org.sdase.commons.server.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;

/**
 * Listener adds or removes Kafka internal metrics for each consumer to a micrometer registry. Made
 * package private to not expose micrometer
 */
class MicrometerConsumerListener {

  private final MeterRegistry meterRegistry;

  private final Map<String, KafkaClientMetrics> metrics = new HashMap<>();

  MicrometerConsumerListener(MeterRegistry meterRegistry) {
    this(meterRegistry, Collections.emptyList());
  }

  MicrometerConsumerListener(MeterRegistry meterRegistry, List<Tag> tags) {
    this.meterRegistry = meterRegistry;
    this.tags = tags;
  }

  private final List<Tag> tags;

  /**
   * Binds Kafka internal metrics to a micrometer registry.
   *
   * @param clientId consumer id
   * @param consumer Kafka internal consumer
   */
  synchronized <K, V> void consumerAdded(String clientId, Consumer<K, V> consumer) {

    this.metrics.computeIfAbsent(
        clientId,
        k -> {
          List<Tag> consumerTags = new ArrayList<>(this.tags);
          KafkaClientMetrics kafkaClientMetrics = new KafkaClientMetrics(consumer, consumerTags);
          kafkaClientMetrics.bindTo(this.meterRegistry);
          return kafkaClientMetrics;
        });
  }

  /**
   * Removes Kafka internal metrics from a micrometer registry.
   *
   * @param clientId consumer id
   */
  synchronized void consumerRemoved(String clientId) {
    KafkaClientMetrics removed = this.metrics.remove(clientId);
    if (removed != null) {
      removed.close();
    }
  }
}
