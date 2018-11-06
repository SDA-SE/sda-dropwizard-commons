package com.sdase.commons.server.prometheus.health;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckFilter;
import com.codahale.metrics.health.HealthCheckRegistry;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.exporter.common.TextFormat;
import org.apache.commons.lang3.Validate;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;

public class HealthCheckAsPrometheusMetricServlet extends HttpServlet {

   private static final long serialVersionUID = 1L;

   private static final String HEALTH_CHECK_STATUS_METRIC = "healthcheck_status";
   private static final List<String> HEALTH_CHECK_METRIC_LABELS = unmodifiableList(singletonList("name"));

   private final HealthCheckRegistry registry;

   public HealthCheckAsPrometheusMetricServlet(HealthCheckRegistry registry) {
      Validate.notNull(registry);
      this.registry = registry;
   }

   @Override
   public void destroy() {
      super.destroy();
      registry.shutdown();
   }

   @Override
   protected void doGet(HttpServletRequest req, HttpServletResponse res) {
      List<Sample> samples = collectHealthCheckSamples();

      MetricFamilySamples metricFamilySamples = new MetricFamilySamples(
            HEALTH_CHECK_STATUS_METRIC,
            Collector.Type.GAUGE,
            "Status of a Health Check (1: healthy, 0: unhealthy)",
            samples);

      res.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");
      res.setStatus(HttpServletResponse.SC_OK);
      res.setContentType(TextFormat.CONTENT_TYPE_004);

      try (Writer writer = res.getWriter()) {
         TextFormat.write004(writer, Collections.enumeration(Collections.singleton(metricFamilySamples)));
      } catch (IOException e) {
         // nothing to do here, sonar likes to have this exception caught: squid:S1989
      }
   }

   private List<Sample> collectHealthCheckSamples() {
      SortedMap<String, HealthCheck.Result> results = registry.runHealthChecks(HealthCheckFilter.ALL);

      return results.entrySet().stream()
            .map(e -> createSample(e.getKey(), e.getValue().isHealthy()))
            .collect(Collectors.toList());
   }

   private Sample createSample(String healthCheckName, boolean healthy) {
      List<String> labelValues = singletonList(healthCheckName);
      double gaugeValue = healthy ? 1.0 : 0.0;
      return new Sample(HEALTH_CHECK_STATUS_METRIC, HEALTH_CHECK_METRIC_LABELS, labelValues, gaugeValue);
   }

}