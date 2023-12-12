package org.sdase.commons.server.prometheus.health;

import static io.dropwizard.metrics.servlets.HealthCheckServlet.HEALTH_CHECK_REGISTRY;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;

/**
 * Serves all {@link HealthCheck}s in a Prometheus metric format.
 *
 * <p>Uses the {@link HealthCheckRegistry} provided in the servlet context as {@link
 * io.dropwizard.metrics.servlets.HealthCheckServlet#HEALTH_CHECK_REGISTRY HEALTH_CHECK_REGISTRY}.
 * This is the same registry that is used to provide {@link HealthCheck}s at the default health
 * check endpoint using {@link io.dropwizard.metrics.servlets.HealthCheckServlet
 * HealthCheckServlet}.
 *
 * <p>Currently all {@link HealthCheck.Result}s are collected synchronously and sequentially unless
 * a {@link HealthCheck} is annotated as {@link com.codahale.metrics.health.annotation.Async Async}
 * when the endpoint is called. To change this behaviour and collect {@link HealthCheck.Result}s in
 * parallel in the default {@link io.dropwizard.metrics.servlets.HealthCheckServlet
 * HealthCheckServlet}, an {@link java.util.concurrent.ExecutorService ExecutorService} has to be
 * registered in the servlet context as {@link
 * io.dropwizard.metrics.servlets.HealthCheckServlet#HEALTH_CHECK_EXECUTOR HEALTH_CHECK_EXECUTOR}.
 * <strong>To use the same executor here as well, additional implementation is needed in this
 * class.</strong>
 *
 * @deprecated Use {@link DropwizardHealthCheckMeters} that is used to provide metrics at
 *     /metrics/prometheus instead.
 */
@Deprecated(forRemoval = true)
public class HealthCheckAsPrometheusMetricServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;
  private static final Set<String> HEALTH_CHECK_STATUS_METRICS_FILTER =
      Set.of(DropwizardHealthCheckMeters.HEALTH_CHECK_STATUS_METRIC);

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) {

    res.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");
    res.setStatus(HttpServletResponse.SC_OK);
    res.setContentType(TextFormat.CONTENT_TYPE_004);

    try (Writer writer = res.getWriter()) {
      var healthCheckSamples =
          CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(
              HEALTH_CHECK_STATUS_METRICS_FILTER);
      TextFormat.write004(writer, healthCheckSamples);
    } catch (IOException e) {
      // nothing to do here, sonar likes to have this exception caught: squid:S1989
    }
  }
}
