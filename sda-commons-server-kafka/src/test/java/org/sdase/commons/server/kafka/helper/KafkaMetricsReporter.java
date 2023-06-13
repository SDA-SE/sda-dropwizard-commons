package org.sdase.commons.server.kafka.helper;

import java.util.List;
import java.util.Map;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class gets called by Kafka Clients when metrics change This is used to update a list of metrics
 * in {@link MetricsHelper} which can be used in tests registered using {@code metric.reporters}
 * setting in Kafka configuration
 */
public class KafkaMetricsReporter implements MetricsReporter {

  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMetricsReporter.class);

  @Override
  public void init(List<KafkaMetric> metrics) {}

  @Override
  public void metricChange(KafkaMetric metric) {
    String metricGroupAndName = metric.metricName().group() + "-" + metric.metricName().name();
    if (!MetricsHelper.getListOfMetrics().contains(metricGroupAndName)) {

      MetricsHelper.addMetric(metricGroupAndName);
    }
  }

  @Override
  public void metricRemoval(KafkaMetric metric) {}

  @Override
  public void close() {}

  @Override
  public void configure(Map<String, ?> configs) {}
}
