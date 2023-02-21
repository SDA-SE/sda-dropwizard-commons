package org.sdase.commons.server.prometheus.health;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import java.util.List;
import java.util.SortedMap;
import java.util.stream.Collectors;

/**
 * Provides all {@link HealthCheck}s as Prometheus metrics.
 *
 * <p>Currently all {@link HealthCheck.Result}s are collected synchronously and sequentially unless
 * a {@link HealthCheck} is annotated as {@link com.codahale.metrics.health.annotation.Async Async}
 * when the endpoint is called.
 */
public class HealthCheckMetricsCollector extends Collector {
  private static final String HEALTH_CHECK_STATUS_METRIC = "healthcheck_status";
  private static final String HEALTH_CHECK_STATUS_OVERALL_METRIC = "healthcheck_status_overall";
  private static final List<String> HEALTH_CHECK_METRIC_LABELS =
      unmodifiableList(singletonList("name"));

  private final HealthCheckRegistry healthCheckRegistry;

  public HealthCheckMetricsCollector(HealthCheckRegistry healthCheckRegistry) {
    this.healthCheckRegistry = healthCheckRegistry;
  }

  public List<MetricFamilySamples> collect() {
    SortedMap<String, HealthCheck.Result> healthCheckResults =
        healthCheckRegistry.runHealthChecks();

    return asList(
        new MetricFamilySamples(
            HEALTH_CHECK_STATUS_METRIC,
            Collector.Type.GAUGE,
            "Status of a Health Check (1: healthy, 0: unhealthy)",
            createHealthCheckSamples(healthCheckResults)),
        new MetricFamilySamples(
            HEALTH_CHECK_STATUS_OVERALL_METRIC,
            Collector.Type.GAUGE,
            "Combined status across all Health Checks (1: healthy, 0: unhealthy)",
            asList(createHealthCheckOverallSample(healthCheckResults))));
  }

  private List<Sample> createHealthCheckSamples(
      SortedMap<String, HealthCheck.Result> healthCheckResults) {
    return healthCheckResults.entrySet().stream()
        .map(e -> createSample(e.getKey(), e.getValue().isHealthy()))
        .collect(Collectors.toList());
  }

  private Sample createHealthCheckOverallSample(
      SortedMap<String, HealthCheck.Result> healthCheckResults) {
    return new Sample(
        HEALTH_CHECK_STATUS_OVERALL_METRIC,
        emptyList(),
        emptyList(),
        healthCheckResults.entrySet().stream()
            .mapToDouble(e -> e.getValue().isHealthy() ? 1d : 0d)
            .min()
            .orElse(0d));
  }

  private Sample createSample(String healthCheckName, boolean healthy) {
    List<String> labelValues = singletonList(healthCheckName);
    double gaugeValue = healthy ? 1.0 : 0.0;
    return new Sample(
        HEALTH_CHECK_STATUS_METRIC, HEALTH_CHECK_METRIC_LABELS, labelValues, gaugeValue);
  }
}
