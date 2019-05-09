package org.sdase.commons.server.kafka.consumer;

import java.util.Map.Entry;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;

public class ConsumerHelper {

  private ConsumerHelper() {
    // do not instantiate
  }

  /**
   * returns the name of the consumer as used in log messages that is hidden within the metrics
   * @param consumer Kafka consumer for that the name is required
   * @return name
   */
  public static <K, V> String getClientId(KafkaConsumer<K, V> consumer) {
    Entry<MetricName, ? extends Metric> entry = consumer.metrics().entrySet().stream().findFirst().orElse(null);
    return entry != null ? entry.getKey().tags().get("client-id") : "";
  }

}
