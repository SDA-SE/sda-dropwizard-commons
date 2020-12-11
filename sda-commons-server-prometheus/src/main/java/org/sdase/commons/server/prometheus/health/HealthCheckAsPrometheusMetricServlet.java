package org.sdase.commons.server.prometheus.health;

import static com.codahale.metrics.servlets.HealthCheckServlet.HEALTH_CHECK_REGISTRY;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Serves all {@link HealthCheck}s in a Prometheus metric format.
 *
 * <p>Uses the {@link HealthCheckRegistry} provided in the servlet context as {@link
 * com.codahale.metrics.servlets.HealthCheckServlet#HEALTH_CHECK_REGISTRY HEALTH_CHECK_REGISTRY}.
 * This is the same registry that is used to provide {@link HealthCheck}s at the default health
 * check endpoint using {@link com.codahale.metrics.servlets.HealthCheckServlet HealthCheckServlet}.
 *
 * <p>Currently all {@link HealthCheck.Result}s are collected synchronously and sequentially unless
 * a {@link HealthCheck} is annotated as {@link com.codahale.metrics.health.annotation.Async Async}
 * when the endpoint is called. To change this behaviour and collect {@link HealthCheck.Result}s in
 * parallel in the default {@link com.codahale.metrics.servlets.HealthCheckServlet
 * HealthCheckServlet}, an {@link java.util.concurrent.ExecutorService ExecutorService} has to be
 * registered in the servlet context as {@link
 * com.codahale.metrics.servlets.HealthCheckServlet#HEALTH_CHECK_EXECUTOR HEALTH_CHECK_EXECUTOR}.
 * <strong>To use the same executor here as well, additional implementation is needed in this
 * class.</strong>
 *
 * @deprecated Use the HealthCheckMetricsCollector that is used to provide metrics at
 *     /metrics/prometheus instead.
 */
@Deprecated
public class HealthCheckAsPrometheusMetricServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  private HealthCheckRegistry registry;
  private HealthCheckMetricsCollector healthCheckMetricsCollector;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    if (registry == null) {
      ServletContext context = config.getServletContext();
      Object registryAttr = context.getAttribute(HEALTH_CHECK_REGISTRY);
      if (registryAttr instanceof HealthCheckRegistry) {
        registry = (HealthCheckRegistry) registryAttr;
      } else {
        throw new ServletException("Couldn't find a HealthCheckRegistry instance.");
      }
    }

    healthCheckMetricsCollector = new HealthCheckMetricsCollector(registry);
  }

  @Override
  public void destroy() {
    super.destroy();
    registry.shutdown();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) {
    List<MetricFamilySamples> metricFamilySamples = healthCheckMetricsCollector.collect();

    res.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");
    res.setStatus(HttpServletResponse.SC_OK);
    res.setContentType(TextFormat.CONTENT_TYPE_004);

    try (Writer writer = res.getWriter()) {
      TextFormat.write004(writer, Collections.enumeration(metricFamilySamples));
    } catch (IOException e) {
      // nothing to do here, sonar likes to have this exception caught: squid:S1989
    }
  }
}
