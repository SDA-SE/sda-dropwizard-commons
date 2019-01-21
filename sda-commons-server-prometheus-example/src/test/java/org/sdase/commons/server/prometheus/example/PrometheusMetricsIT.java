package org.sdase.commons.server.prometheus.example;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.starter.SdaPlatformConfiguration;
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

public class PrometheusMetricsIT {

   private static final Logger LOG = LoggerFactory.getLogger(PrometheusMetricsIT.class);

   @ClassRule
   public static final DropwizardAppRule<SdaPlatformConfiguration> DW = DropwizardRuleHelper
         .dropwizardTestAppFrom(MetricExampleApp.class)
         .withConfigFrom(SdaPlatformConfiguration::new)
         .withRandomPorts()
         .build();

   @Test
   public void produceGaugeMetric() {

      String metrics = readMetrics();

      assertThat(metrics)
            .contains("# HELP some_operation_temperature_celsius Tracks the temperature recorded within the operation.")
            .contains("# TYPE some_operation_temperature_celsius gauge");

   }

   @Test
   public void produceSuccessCounterMetric() {

      String metrics = readMetrics();

      assertThat(metrics)
            .contains("# HELP some_operation_success_counter Counts successes occurred when some operation is invoked.")
            .contains("# TYPE some_operation_success_counter counter");

   }

   @Test
   public void produceErrorCounterMetric() {

      String metrics = readMetrics();

      assertThat(metrics)
            .contains("# HELP some_operation_error_counter Counts errors occurred when some operation is invoked.")
            .contains("# TYPE some_operation_error_counter counter");

   }

   @Test
   public void produceHistogramMetric() {

      String metrics = readMetrics();

      assertThat(metrics)
            .contains("# HELP some_operation_execution_duration_seconds Tracks duration of some operation.")
            .contains("# TYPE some_operation_execution_duration_seconds histogram");

   }

   private String readMetrics() {
      Response response = DW
            .client()
            .target(String.format("http://localhost:%d", DW.getAdminPort()) + "/metrics/prometheus")
            .request()
            .get();
      String metrics = response.readEntity(String.class);
      LOG.info("Prometheus metrics:\n{}", metrics);
      return metrics;
   }

}