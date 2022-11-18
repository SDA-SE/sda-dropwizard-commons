package org.sdase.commons.server.prometheus.example;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.starter.SdaPlatformConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PrometheusMetricsIT {

  private static final Logger LOG = LoggerFactory.getLogger(PrometheusMetricsIT.class);

  @RegisterExtension
  static final DropwizardAppExtension<SdaPlatformConfiguration> DW =
      new DropwizardAppExtension<>(
          MetricExampleApp.class,
          null,
          // use random ports so that tests can run in parallel
          // and do not affect each other when one is not shutting down
          randomPorts());

  @Test
  void produceGaugeMetric() {

    String metrics = readMetrics();

    assertThat(metrics)
        .contains(
            "# HELP some_operation_temperature_celsius Tracks the temperature recorded within the operation.")
        .contains("# TYPE some_operation_temperature_celsius gauge");
  }

  @Test
  void produceSuccessCounterMetric() {

    String metrics = readMetrics();

    assertThat(metrics)
        .contains(
            "# HELP some_operation_success_counter_total Counts successes occurred when some operation is invoked.")
        .contains("# TYPE some_operation_success_counter_total counter");
  }

  @Test
  void produceErrorCounterMetric() {

    String metrics = readMetrics();

    assertThat(metrics)
        .contains(
            "# HELP some_operation_error_counter_total Counts errors occurred when some operation is invoked.")
        .contains("# TYPE some_operation_error_counter_total counter");
  }

  @Test
  void produceHistogramMetric() {

    String metrics = readMetrics();

    assertThat(metrics)
        .contains(
            "# HELP some_operation_execution_duration_seconds Tracks duration of some operation.")
        .contains("# TYPE some_operation_execution_duration_seconds histogram");
  }

  private String readMetrics() {
    Response response =
        DW.client()
            .target(String.format("http://localhost:%d", DW.getAdminPort()) + "/metrics/prometheus")
            .request()
            .get();
    String metrics = response.readEntity(String.class);
    LOG.info("Prometheus metrics:\n{}", metrics);
    return metrics;
  }
}
