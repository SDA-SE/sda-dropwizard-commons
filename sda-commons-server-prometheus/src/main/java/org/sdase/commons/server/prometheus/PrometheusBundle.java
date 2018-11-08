package org.sdase.commons.server.prometheus;

import org.sdase.commons.server.prometheus.health.HealthCheckAsPrometheusMetricServlet;
import org.sdase.commons.server.prometheus.metric.request.duration.RequestDurationFilter;
import org.sdase.commons.server.prometheus.metric.request.duration.RequestDurationHistogramSpecification;
import io.dropwizard.Bundle;
import io.dropwizard.lifecycle.Managed;
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

      // Bridge all metrics
      DropwizardExports dropwizardExports = new DropwizardExports(environment.metrics());
      CollectorRegistry.defaultRegistry.register(dropwizardExports);

      environment.lifecycle().manage(new Managed() {
         @Override
         public void start() {
            // nothing here
         }

         @Override
         public void stop() {
            requestDurationHistogramSpecification.unregister();
            CollectorRegistry.defaultRegistry.unregister(dropwizardExports);
         }
      });

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
