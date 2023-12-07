package org.sdase.commons.server.prometheus.health;

import static io.prometheus.client.Collector.Type.GAUGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codahale.metrics.health.HealthCheck.Result;
import com.codahale.metrics.health.HealthCheckRegistry;
import io.prometheus.client.Collector.MetricFamilySamples;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class HealthCheckMetricsCollectorTest {

  @Test
  void shouldExposeMetricsForHealthChecks() {
    HealthCheckRegistry registry = mock(HealthCheckRegistry.class);
    HealthCheckMetricsCollector collector = new HealthCheckMetricsCollector(registry);

    SortedMap<String, Result> results = new TreeMap<>();
    results.put("healthy_check", Result.healthy());
    results.put("unhealthy_check", Result.unhealthy("Something went wrong"));
    when(registry.runHealthChecks()).thenReturn(results);

    List<MetricFamilySamples> metrics = collector.collect();

    assertThat(metrics).hasSize(1);
    MetricFamilySamples gauge = metrics.get(0);
    assertThat(gauge.name).isEqualTo("healthcheck_status");
    assertThat(gauge.help).isNotBlank();
    assertThat(gauge.type).isEqualTo(GAUGE);
    assertThat(gauge.samples).hasSize(2);

    MetricFamilySamples.Sample sampleHealthy = gauge.samples.get(0);
    assertThat(sampleHealthy.name).isEqualTo("healthcheck_status");
    assertThat(sampleHealthy.labelNames).containsExactly("name");
    assertThat(sampleHealthy.labelValues).containsExactly("healthy_check");
    assertThat(sampleHealthy.value).isEqualTo(1.0);

    MetricFamilySamples.Sample sampleUnhealthy = gauge.samples.get(1);
    assertThat(sampleUnhealthy.name).isEqualTo("healthcheck_status");
    assertThat(sampleUnhealthy.labelNames).containsExactly("name");
    assertThat(sampleUnhealthy.labelValues).containsExactly("unhealthy_check");
    assertThat(sampleUnhealthy.value).isEqualTo(0.0);
  }
}
