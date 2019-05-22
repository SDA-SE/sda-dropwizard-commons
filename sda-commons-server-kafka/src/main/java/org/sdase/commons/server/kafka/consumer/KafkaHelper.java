package org.sdase.commons.server.kafka.consumer;

import java.util.Map.Entry;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;

public class KafkaHelper {

  private KafkaHelper() {
    // do not instantiate
  }

  /**
   * @param consumer the Kafka consumer
   * @return the name of the consumer as used in log messages that is hidden within the metrics
   */
  public static <K, V> String getClientId(KafkaConsumer<K, V> consumer) {
    Entry<MetricName, ? extends Metric> entry = consumer.metrics().entrySet().stream().findFirst().orElse(null);
    return entry != null ? entry.getKey().tags().get("client-id") : "";
  }

  /**
   * @param producer the Kafka producer
   * @return the name of the producer as used in log messages that is hidden within the metrics
   */
  public static <K, V> String getClientId(KafkaProducer<K, V> producer) {
    Entry<MetricName, ? extends Metric> entry = producer.metrics().entrySet().stream().findFirst().orElse(null);
    return entry != null ? entry.getKey().tags().get("client-id") : "";
  }

}
