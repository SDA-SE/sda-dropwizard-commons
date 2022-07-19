package org.sdase.commons.server.kafka.prometheus;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.sdase.commons.server.kafka.consumer.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom Prometheus Collector scraping Kafka Metrics for all registered message listeners.<br>
 * Specify the metrics to be handled by Prometheus in {@link #KAFKA_METRICS}.
 */
public class KafkaConsumerMetrics extends Collector {

  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumerMetrics.class);

  /** The prefix for the metrics name as they are published to Prometheus */
  private static final String METRIC_NAME_PREFIX = "kafka_consumer_";

  /**
   * <a href= "https://kafka.apache.org/documentation/#consumer_monitoring">Kafka Metrics</a>
   * scraped by this class.
   */
  private static final String[] KAFKA_METRICS = {"records-lag"};

  /**
   * The labels added by {@link KafkaConsumerMetrics}. The labels and their order have to be aligned
   * with the values created in {@link #createLabelValuesForCurrentMessage(String, String, String)}
   */
  private static final String[] LABELS = {
    // id of the client retrieving the Kafka metric
    "consumer_name"
  };

  private static final String KAFKA_METRIC_TAG_CLIENT_ID = "client-id";

  private List<MessageListener<?, ?>> messageListeners;

  /**
   * Creates and registers a custom Prometheus Collector {@link Collector}. <strong>Note that there
   * should be only one registered instance of this type in the application.</strong>
   *
   * @param messageListeners list of listeners for which to gather metrics
   */
  public KafkaConsumerMetrics(List<MessageListener<?, ?>> messageListeners) {
    this.messageListeners = messageListeners;
    this.register();
    LOGGER.debug("Registered Kafka Consumer Metrics Collector.");
  }

  @Override
  public List<MetricFamilySamples> collect() {
    List<MetricFamilySamples> mfs = new ArrayList<>();
    messageListeners.forEach(
        listener -> mfs.addAll(collectGaugesPerListener(listener.getConsumer().metrics())));
    return mfs;
  }

  private List<GaugeMetricFamily> collectGaugesPerListener(
      Map<MetricName, ? extends Metric> metrics) {
    List<GaugeMetricFamily> list = new ArrayList<>();

    Arrays.stream(KAFKA_METRICS)
        .forEach(
            metricName -> list.add(collectKafkaMetric(metricByNameOrNull(metrics, metricName))));
    list.removeIf(Objects::isNull);
    return list;
  }

  private GaugeMetricFamily collectKafkaMetric(Entry<MetricName, ? extends Metric> kafkaMetric) {
    if (kafkaMetric == null) {
      return null;
    }
    String normalizedMetricName =
        METRIC_NAME_PREFIX + kafkaMetric.getKey().name().replace('-', '_');
    GaugeMetricFamily labeledGauge =
        new GaugeMetricFamily(
            normalizedMetricName, kafkaMetric.getKey().description(), Arrays.asList(LABELS));

    String[] labelValues =
        createLabelValuesForCurrentMessage(
            kafkaMetric.getKey().tags().get(KAFKA_METRIC_TAG_CLIENT_ID));

    labeledGauge.addMetric(
        Arrays.asList(labelValues), (double) kafkaMetric.getValue().metricValue());
    return labeledGauge;
  }

  /**
   * Creates all values for the labels required by the gauges in appropriate order.
   *
   * @param consumerName
   * @return all values for the labels in the order they are registered in the respective gauge
   */
  @SuppressWarnings("static-method")
  private String[] createLabelValuesForCurrentMessage(String consumerName) {
    // the number of values and their order has to be aligned with the
    // defined #LABELS
    return new String[] {consumerName};
  }

  /**
   * Extracts {@link Metric} from Kafka Metrics by its name
   *
   * @param metrics
   * @param name
   * @return
   */
  @SuppressWarnings("static-method")
  private Entry<MetricName, ? extends Metric> metricByNameOrNull(
      Map<MetricName, ? extends Metric> metrics, String name) {
    return metrics.entrySet().stream()
        .filter(mx -> mx.getKey().name().equals(name))
        .findFirst()
        .orElse(null);
  }
}
