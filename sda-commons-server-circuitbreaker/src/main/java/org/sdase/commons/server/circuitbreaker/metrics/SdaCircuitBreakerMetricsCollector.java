package org.sdase.commons.server.circuitbreaker.metrics;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.prometheus.AbstractCircuitBreakerMetrics;
import java.util.Collections;
import java.util.List;

/**
 * Replaces {@link io.github.resilience4j.prometheus.collectors.CircuitBreakerMetricsCollector} but
 * adds only resilience4j_circuitbreaker_calls_bucket metric.
 *
 * <p>Prometheus and Micrometer handle histogram metrics differently. Prometheus adds a
 * resilience4j_circuitbreaker_calls_bucket histogram while micrometer adds a
 * resilience4j_circuitbreaker_calls_count summary.
 *
 * <p>This class is adding only the prometheus histogram metric to not introduce a breaking changes.
 * The rest of the metrics are added by the resilience4j micrometer library {@link
 * io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics}.
 *
 * @deprecated class should be removed in the next major release. This will introduce breaking
 *     changes since the resilience4j_circuitbreaker_calls_bucket histogram will not be exposed
 *     anymore.
 */
@Deprecated(since = "5.8.1", forRemoval = true)
public class SdaCircuitBreakerMetricsCollector extends AbstractCircuitBreakerMetrics {

  private SdaCircuitBreakerMetricsCollector(
      MetricNames names, MetricOptions options, CircuitBreakerRegistry circuitBreakerRegistry) {
    super(names, options);

    for (CircuitBreaker circuitBreaker : circuitBreakerRegistry.getAllCircuitBreakers()) {
      addMetrics(circuitBreaker);
    }
    circuitBreakerRegistry
        .getEventPublisher()
        .onEntryAdded(event -> addMetrics(event.getAddedEntry()));
  }

  /**
   * Creates a new collector using given {@code registry} as source of circuit breakers.
   *
   * @param circuitBreakerRegistry the source of circuit breakers
   */
  public static SdaCircuitBreakerMetricsCollector ofCircuitBreakerRegistry(
      CircuitBreakerRegistry circuitBreakerRegistry) {
    return new SdaCircuitBreakerMetricsCollector(
        MetricNames.ofDefaults(), MetricOptions.ofDefaults(), circuitBreakerRegistry);
  }

  @Override
  public List<MetricFamilySamples> collect() {
    return Collections.list(collectorRegistry.metricFamilySamples());
  }
}
