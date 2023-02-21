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
import org.junit.Before;
import org.junit.Test;

public class HealthCheckMetricsCollectorTest {

  private HealthCheckMetricsCollector collector = null;
  private HealthCheckRegistry registry = null;

  @Before
  public void setUp() {
    registry = mock(HealthCheckRegistry.class);
    collector = new HealthCheckMetricsCollector(registry);
  }

  private void initHealthcheckRegistryMock(int numHealthy, int numUnhealthy) {
    SortedMap<String, Result> results = new TreeMap<>();
    for (int i = 0; i < numHealthy; i++) {
      results.put("healthy_check_" + i, Result.healthy());
    }
    for (int i = 0; i < numUnhealthy; i++) {
      results.put("unhealthy_check_" + i, Result.unhealthy("Something went wrong"));
    }
    when(registry.runHealthChecks()).thenReturn(results);
  }

  @Test
  public void shouldExposeMetricsForHealthChecks() {
    initHealthcheckRegistryMock(1, 1);
    List<MetricFamilySamples> metrics = collector.collect();

    assertThat(metrics).hasSize(2);
    MetricFamilySamples gauge = metrics.get(0);
    assertThat(gauge.name).isEqualTo("healthcheck_status");
    assertThat(gauge.help).isNotBlank();
    assertThat(gauge.type).isEqualTo(GAUGE);
    assertThat(gauge.samples).hasSize(2);

    MetricFamilySamples.Sample sampleHealthy = gauge.samples.get(0);
    assertThat(sampleHealthy.name).isEqualTo("healthcheck_status");
    assertThat(sampleHealthy.labelNames).containsExactly("name");
    assertThat(sampleHealthy.labelValues).containsExactly("healthy_check_0");
    assertThat(sampleHealthy.value).isEqualTo(1.0);

    MetricFamilySamples.Sample sampleUnhealthy = gauge.samples.get(1);
    assertThat(sampleUnhealthy.name).isEqualTo("healthcheck_status");
    assertThat(sampleUnhealthy.labelNames).containsExactly("name");
    assertThat(sampleUnhealthy.labelValues).containsExactly("unhealthy_check_0");
    assertThat(sampleUnhealthy.value).isEqualTo(0.0);
  }

  @Test
  public void shouldExposeMetricsForHealthChecksOverallHealthy() {
    initHealthcheckRegistryMock(1, 0);
    List<MetricFamilySamples> metrics = collector.collect();

    assertThat(metrics).hasSize(2);
    MetricFamilySamples gaugeOverall = metrics.get(1);
    assertThat(gaugeOverall.name).isEqualTo("healthcheck_status_overall");
    assertThat(gaugeOverall.help).isNotBlank();
    assertThat(gaugeOverall.type).isEqualTo(GAUGE);
    assertThat(gaugeOverall.samples).hasSize(1);

    MetricFamilySamples.Sample sampleHealthyOverall = gaugeOverall.samples.get(0);
    assertThat(sampleHealthyOverall.name).isEqualTo("healthcheck_status_overall");
    assertThat(sampleHealthyOverall.labelNames).isEmpty();
    assertThat(sampleHealthyOverall.labelValues).isEmpty();
    assertThat(sampleHealthyOverall.value).isEqualTo(1.0);
  }

  @Test
  public void shouldExposeMetricsForHealthChecksOverallUnhealthy() {
    initHealthcheckRegistryMock(0, 1);
    List<MetricFamilySamples> metrics = collector.collect();

    MetricFamilySamples gaugeOverall = metrics.get(1);
    assertThat(gaugeOverall.name).isEqualTo("healthcheck_status_overall");
    assertThat(gaugeOverall.help).isNotBlank();
    assertThat(gaugeOverall.type).isEqualTo(GAUGE);
    assertThat(gaugeOverall.samples).hasSize(1);

    MetricFamilySamples.Sample sampleHealthyOverall = gaugeOverall.samples.get(0);
    assertThat(sampleHealthyOverall.name).isEqualTo("healthcheck_status_overall");
    assertThat(sampleHealthyOverall.labelNames).isEmpty();
    assertThat(sampleHealthyOverall.labelValues).isEmpty();
    assertThat(sampleHealthyOverall.value).isEqualTo(0.0);
  }

  @Test
  public void shouldExposeMetricsForHealthChecksOverallMixed() {
    initHealthcheckRegistryMock(7, 1);
    List<MetricFamilySamples> metrics = collector.collect();

    MetricFamilySamples gaugeOverall = metrics.get(1);
    assertThat(gaugeOverall.name).isEqualTo("healthcheck_status_overall");
    assertThat(gaugeOverall.help).isNotBlank();
    assertThat(gaugeOverall.type).isEqualTo(GAUGE);
    assertThat(gaugeOverall.samples).hasSize(1);

    MetricFamilySamples.Sample sampleHealthyOverall = gaugeOverall.samples.get(0);
    assertThat(sampleHealthyOverall.name).isEqualTo("healthcheck_status_overall");
    assertThat(sampleHealthyOverall.labelNames).isEmpty();
    assertThat(sampleHealthyOverall.labelValues).isEmpty();
    assertThat(sampleHealthyOverall.value).isEqualTo(0.0);
  }
}
