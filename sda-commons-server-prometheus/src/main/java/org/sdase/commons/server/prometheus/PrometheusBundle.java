package org.sdase.commons.server.prometheus;

import io.prometheus.client.dropwizard.samplebuilder.SampleBuilder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.sdase.commons.server.prometheus.health.HealthCheckAsPrometheusMetricServlet;
import org.sdase.commons.server.prometheus.mapping.DropwizardSampleBuilder;
import org.sdase.commons.server.prometheus.mapping.MapperConfig;
import org.sdase.commons.server.prometheus.metric.request.duration.RequestDurationFilter;
import org.sdase.commons.server.prometheus.metric.request.duration.RequestDurationHistogramSpecification;
import io.dropwizard.Bundle;
import io.dropwizard.setup.AdminEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.MetricsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRegistration;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

import static org.sdase.commons.server.dropwizard.lifecycle.ManagedShutdownListener.onShutdown;

/**
 * <p>
 *    This bundle activates prometheus monitoring and registers a servlet on the admin port from where Prometheus
 *    scrapes the gathered metrics.
 * </p>
 * <p>
 *    To activate the bundle, there is a {@link #builder()} to be used in the
 *    {@link io.dropwizard.Application#initialize(Bootstrap) initialize} method:
 * </p>
 * <pre>
 *    <code>public void initialize(final Bootstrap<AppConfig> bootstrap) {
 *       // ...
 *       bootstrap.addBundle(PrometheusBundle.builder().withPrometheusConfigProvider(AppConfig::getPrometheus).build());
 *       // ...
 *    }
 *    </code>
 * </pre>
 *
 */
public class PrometheusBundle implements Bundle, DynamicFeature {

   // sonar: this path is used as a convention in our world!
   private static final String METRICS_SERVLET_URL = "/metrics/prometheus"; // NOSONAR
   // sonar: this path is used as a convention in our world!
   private static final String HEALTH_SERVLET_URL = "/healthcheck/prometheus"; // NOSONAR

   private static final Logger LOG = LoggerFactory.getLogger(PrometheusBundle.class);

   private RequestDurationHistogramSpecification requestDurationHistogramSpecification;

   // use PrometheusBundle.builder()... to get an instance
   private PrometheusBundle() {
   }

   @Override
   public void run(Environment environment) {

      registerMetricsServlet(environment.admin());
      registerHealthCheckServlet(environment.admin());
      environment.jersey().register(this);

      // init Histogram at startup
      requestDurationHistogramSpecification = new RequestDurationHistogramSpecification();
      initializeDropwizardMetricsBridge(environment);

   }

   private void initializeDropwizardMetricsBridge(Environment environment) {
      // Create a custom mapper to convert the Graphite style Dropwizard metrics
      // to Prometheus metrics.
      List<MapperConfig> mappers = createMetricsMapperConfigs();

      SampleBuilder sampleBuilder = new DropwizardSampleBuilder(mappers);
      DropwizardExports dropwizardExports = new DropwizardExports(environment.metrics(), sampleBuilder);
      CollectorRegistry.defaultRegistry.register(dropwizardExports);

      environment.lifecycle().manage(onShutdown(() -> {
         requestDurationHistogramSpecification.unregister();
         CollectorRegistry.defaultRegistry.unregister(dropwizardExports);
      }));
   }

   private List<MapperConfig> createMetricsMapperConfigs() {
      List<MapperConfig> mappers = new ArrayList<>();
      mappers.add(createMapperConfig("ch.qos.logback.core.*.*", "logback_appender", "name", "level"));
      mappers.add(createMapperConfig("jvm.gc.*.count", "jvm_gc_total", "step"));
      mappers.add(createMapperConfig("jvm.gc.*.time", "jvm_gc_seconds", "step"));
      mappers.add(createMapperConfig("jvm.memory.pools.*.committed", "jvm_memory_pools_committed_bytes", "pool"));
      mappers.add(createMapperConfig("jvm.memory.pools.*.init", "jvm_memory_pools_init_bytes", "pool"));
      mappers.add(createMapperConfig("jvm.memory.pools.*.max", "jvm_memory_pools_max_bytes", "pool"));
      mappers.add(createMapperConfig("jvm.memory.pools.*.used", "jvm_memory_pools_used_bytes", "pool"));
      mappers.add(createMapperConfig("jvm.memory.pools.*.usage", "jvm_memory_pools_usage_ratio", "pool"));
      mappers.add(createMapperConfig("jvm.memory.pools.*.used-after-gc", "jvm_memory_pools_used_after_gc_bytes", "pool"));
      mappers.add(createMapperConfig("org.apache.http.conn.*.*.available-connections", "apache_http_client_connections", "manager", "name", new AbstractMap.SimpleImmutableEntry<>("state", "available"))); // NOSONAR
      mappers.add(createMapperConfig("org.apache.http.conn.*.*.leased-connections", "apache_http_client_connections", "manager", "name", new AbstractMap.SimpleImmutableEntry<>("state", "leased")));
      mappers.add(createMapperConfig("org.apache.http.conn.*.*.max-connections", "apache_http_client_connections", "manager", "name", new AbstractMap.SimpleImmutableEntry<>("state", "max")));
      mappers.add(createMapperConfig("org.apache.http.conn.*.*.pending-connections", "apache_http_client_connections", "manager", "name", new AbstractMap.SimpleImmutableEntry<>("state", "pending")));
      mappers.add(createMapperConfig("org.apache.http.client.*.*.get-requests", "apache_http_client_request_duration_seconds", "manager", "name", new AbstractMap.SimpleImmutableEntry<>("method", "get"))); // NOSONAR
      mappers.add(createMapperConfig("org.apache.http.client.*.*.post-requests", "apache_http_client_request_duration_seconds", "manager", "name", new AbstractMap.SimpleImmutableEntry<>("method", "post")));
      mappers.add(createMapperConfig("org.apache.http.client.*.*.put-requests", "apache_http_client_request_duration_seconds", "manager", "name", new AbstractMap.SimpleImmutableEntry<>("method", "put")));
      mappers.add(createMapperConfig("org.apache.http.client.*.*.delete-requests", "apache_http_client_request_duration_seconds", "manager", "name", new AbstractMap.SimpleImmutableEntry<>("method", "delete")));
      mappers.add(createMapperConfig("org.apache.http.client.*.*.head-requests", "apache_http_client_request_duration_seconds", "manager", "name", new AbstractMap.SimpleImmutableEntry<>("method", "head")));
      mappers.add(createMapperConfig("org.apache.http.client.*.*.connect-requests", "apache_http_client_request_duration_seconds", "manager", "name", new AbstractMap.SimpleImmutableEntry<>("method", "connect")));
      mappers.add(createMapperConfig("org.apache.http.client.*.*.options-requests", "apache_http_client_request_duration_seconds", "manager", "name", new AbstractMap.SimpleImmutableEntry<>("method", "options")));
      mappers.add(createMapperConfig("org.apache.http.client.*.*.trace-requests", "apache_http_client_request_duration_seconds", "manager", "name", new AbstractMap.SimpleImmutableEntry<>("method", "trace")));
      mappers.add(createMapperConfig("org.eclipse.jetty.server.*.*.connections", "jetty_connections", "factory", "port"));
      mappers.add(createMapperConfig("org.eclipse.jetty.util.thread.*.*.jobs", "jetty_util_thread_jobs_count", "type", "pool"));
      mappers.add(createMapperConfig("org.eclipse.jetty.util.thread.*.*.size", "jetty_util_thread_size_count", "type", "pool"));
      mappers.add(createMapperConfig("org.eclipse.jetty.util.thread.*.*.utilization", "jetty_util_thread_utilization_ratio", "type", "pool"));
      mappers.add(createMapperConfig("org.eclipse.jetty.util.thread.*.*.utilization-max", "jetty_util_thread_max_utilization_ratio", "type", "pool"));
      mappers.add(createMapperConfig("io.dropwizard.jetty.*.active-dispatches", "jetty_handler_active_dispatches_total", "handler")); // NOSONAR
      mappers.add(createMapperConfig("io.dropwizard.jetty.*.active-requests", "jetty_handler_active_requests_total", "handler"));
      mappers.add(createMapperConfig("io.dropwizard.jetty.*.active-suspended", "jetty_handler_active_suspended_total", "handler"));
      mappers.add(createMapperConfig("io.dropwizard.jetty.*.async-dispatches", "jetty_handler_async_dispatches", "handler"));
      mappers.add(createMapperConfig("io.dropwizard.jetty.*.async-timeouts", "jetty_handler_async_timeouts", "handler"));
      return mappers;
   }

   private MapperConfig createMapperConfig(String match, String name, Object... labelNames) {
      MapperConfig config = new MapperConfig();
      config.setMatch(match);
      config.setName(name);
      Map<String, String> labels = new HashMap<>();
      for (int i = 0; i < labelNames.length; ++i) {
         Object labelName = labelNames[i];

         if (labelName instanceof AbstractMap.Entry) {
            AbstractMap.Entry pair = (Entry) labelName;
            labels.put(pair.getKey().toString(), pair.getValue().toString());
         } else {
            labels.put(labelName.toString(), "${" + i + "}");
         }

      }
      config.setLabels(labels);
      return config;
   }

   @Override
   public void configure(ResourceInfo resourceInfo, FeatureContext context) {
      context.register(new RequestDurationFilter(resourceInfo, requestDurationHistogramSpecification));
      LOG.debug("Registered RequestDurationFilter for method {}.", resourceInfo.getResourceMethod());
   }

   private void registerMetricsServlet(AdminEnvironment environment) {
      // Prometheus Servlet registration
      ServletRegistration.Dynamic dynamic = environment.addServlet("metrics", MetricsServlet.class);
      dynamic.addMapping(METRICS_SERVLET_URL);
      LOG.info("Registered Prometheus metrics servlet at '{}'", METRICS_SERVLET_URL);
   }

   private void registerHealthCheckServlet(AdminEnvironment environment) {
      environment
            .addServlet("Health Check as Prometheus Metrics", new HealthCheckAsPrometheusMetricServlet())
            .addMapping(HEALTH_SERVLET_URL);
   }

   @Override
   public void initialize(Bootstrap<?> bootstrap) {
      // Nothing here
   }

   public static InitialBuilder builder() {
      return new Builder();
   }

   //
   // Builder
   //

   public interface InitialBuilder {
      PrometheusBundle build();
   }

   public static class Builder implements InitialBuilder {

      private Builder() {
      }

      public PrometheusBundle build() {
         return new PrometheusBundle();
      }
   }
}
