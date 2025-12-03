package org.sdase.commons.server.prometheus;

import static org.sdase.commons.server.dropwizard.lifecycle.ManagedShutdownListener.onShutdown;

import io.dropwizard.core.Configuration;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.AdminEnvironment;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jersey.server.DefaultJerseyTagsProvider;
import io.micrometer.core.instrument.binder.jersey.server.MetricsApplicationEventListener;
import io.micrometer.core.instrument.binder.jetty.JettyConnectionMetrics;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.dropwizard.samplebuilder.CustomMappingSampleBuilder;
import io.prometheus.client.dropwizard.samplebuilder.MapperConfig;
import io.prometheus.client.dropwizard.samplebuilder.SampleBuilder;
import io.prometheus.client.servlet.jakarta.exporter.MetricsServlet;
import jakarta.servlet.ServletRegistration;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import org.sdase.commons.server.prometheus.config.PrometheusConfiguration;
import org.sdase.commons.server.prometheus.health.DropwizardHealthCheckMeters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This bundle activates prometheus monitoring and registers a servlet on the admin port from where
 * Prometheus scrapes the gathered metrics.
 *
 * <p>To activate the bundle, there is a {@link #builder()} to be used in the {@link
 * io.dropwizard.core.Application#initialize(Bootstrap) initialize} method:
 *
 * <pre>{@code
 * public void initialize(final Bootstrap<AppConfig> bootstrap) {
 *   // ...
 *   bootstrap.addBundle(PrometheusBundle.builder().withPrometheusConfigProvider(AppConfig::getPrometheus).build());
 *   // ...
 * }
 * }</pre>
 */
public class PrometheusBundle<P extends Configuration> implements ConfiguredBundle<P> {

  // sonar: this path is used as a convention in our world!
  private static final String METRICS_SERVLET_URL = "/metrics/prometheus"; // NOSONAR

  private static final Logger LOG = LoggerFactory.getLogger(PrometheusBundle.class);
  public static final String APACHE_HTTP_CLIENT_CONNECTIONS = "apache_http_client_connections";
  public static final String MANAGER = "manager";
  public static final String APACHE_HTTP_CLIENT_REQUEST_DURATION_SECONDS =
      "apache_http_client_request_duration_seconds";

  private static final String DEPRECATED = "deprecated";
  private final Function<P, PrometheusConfiguration> configurationProvider;

  // use PrometheusBundle.builder()... to get an instance
  private PrometheusBundle(PrometheusConfigurationProvider<P> configurationProvider) {
    this.configurationProvider = configurationProvider;
  }

  @Override
  public void run(P configuration, Environment environment) {
    PrometheusConfiguration prometheusConfiguration = configurationProvider.apply(configuration);
    registerMetricsServlet(environment.admin());
    registerHealthCheckMetrics(environment);
    environment.jersey().register(this);

    registerMeterFilter(prometheusConfiguration);

    initializeDropwizardMetricsBridge(environment);

    createPrometheusRegistry(environment);
    bindJvmAndSystemMetricsToGlobalRegistry(environment);
  }

  private void registerMeterFilter(PrometheusConfiguration prometheusConfiguration) {
    if ((prometheusConfiguration.getRequestPercentiles() == null
            || prometheusConfiguration.getRequestPercentiles().isEmpty())
        && !prometheusConfiguration.isEnableRequestHistogram()) {
      LOG.info("No request percentiles specified and histogram disabled.");
      return;
    }
    if (prometheusConfiguration.getRequestPercentiles() != null
        && !prometheusConfiguration.getRequestPercentiles().isEmpty()
        && prometheusConfiguration.isEnableRequestHistogram()) {
      LOG.warn(
          "It is only possible to show the percentiles or the histogram for a metric in prometheus. Only the percentiles will be configured.");
    }
    Metrics.globalRegistry
        .config()
        .meterFilter(
            new MeterFilter() {
              @Override
              public DistributionStatisticConfig configure(
                  Meter.Id id, DistributionStatisticConfig config) {
                if (id.getName().startsWith("http.server.requests")) {
                  return configureDistributionStatisticConfig(config, prometheusConfiguration);
                }
                return config;
              }
            });
  }

  private static DistributionStatisticConfig configureDistributionStatisticConfig(
      DistributionStatisticConfig config, PrometheusConfiguration prometheusConfiguration) {
    DistributionStatisticConfig.Builder builder = DistributionStatisticConfig.builder();
    if (prometheusConfiguration.getRequestPercentiles() != null
        && !prometheusConfiguration.getRequestPercentiles().isEmpty()) {
      double[] arrayPrimitive =
          convertDoubleListToPrimitiveArray(prometheusConfiguration.getRequestPercentiles());
      if (arrayPrimitive.length > 0) {
        builder.percentiles(arrayPrimitive);
      }
    } else {
      builder.percentilesHistogram(true);
      if (prometheusConfiguration.getRequestSlos() != null) {
        builder.serviceLevelObjectives(
            convertDoubleListToPrimitiveArray(prometheusConfiguration.getRequestSlos()));
      }
    }

    return builder
        .percentilePrecision(prometheusConfiguration.getRequestDigitsOfPrecision())
        .build()
        .merge(config);
  }

  private static double[] convertDoubleListToPrimitiveArray(List<Double> doubleList) {
    return doubleList.stream().filter(Objects::nonNull).mapToDouble(d -> d).toArray();
  }

  /**
   * Creates a micrometer PrometheusMeterRegistry and adds it to the Micrometer Global registry. Can
   * be used in a bundle via {@link io.micrometer.core.instrument.Metrics#globalRegistry}
   */
  private void createPrometheusRegistry(Environment environment) {
    PrometheusMeterRegistry meterRegistry =
        new PrometheusMeterRegistry(key -> null, CollectorRegistry.defaultRegistry, Clock.SYSTEM);

    Metrics.addRegistry(meterRegistry);

    environment
        .lifecycle()
        .manage(
            onShutdown(
                () -> {
                  Metrics.removeRegistry(meterRegistry);
                  Metrics.globalRegistry.close();
                  Metrics.globalRegistry.clear();
                }));
  }

  private static void bindJvmAndSystemMetricsToGlobalRegistry(Environment environment) {
    // JVM and System Metrics
    new JvmMemoryMetrics().bindTo(Metrics.globalRegistry);
    new ProcessorMetrics().bindTo(Metrics.globalRegistry);
    new JvmThreadMetrics().bindTo(Metrics.globalRegistry);
    new ClassLoaderMetrics().bindTo(Metrics.globalRegistry);
    // ignore Sonar and not using "try-with-resources" pattern to prevent closing of JVMMetrics
    // otherwise jvm.gc.pause will not be available
    //noinspection resource
    JvmGcMetrics jvmGcMetrics = new JvmGcMetrics(); // NOSONAR
    jvmGcMetrics.bindTo(Metrics.globalRegistry);
    environment.lifecycle().manage(onShutdown(jvmGcMetrics::close));
    // request metrics
    environment
        .jersey()
        .getResourceConfig()
        .register(
            new MetricsApplicationEventListener(
                Metrics.globalRegistry,
                new DefaultJerseyTagsProvider(),
                "http.server.requests",
                true));

    environment
        .lifecycle()
        .addServerLifecycleListener(
            server ->
                JettyConnectionMetrics.addToAllConnectors(server, Metrics.globalRegistry, null));
  }

  private void initializeDropwizardMetricsBridge(Environment environment) {
    // Create a custom mapper to convert the Graphite style Dropwizard metrics
    // to Prometheus metrics.
    List<MapperConfig> mappers = createMetricsMapperConfigs();

    SampleBuilder sampleBuilder = new CustomMappingSampleBuilder(mappers);
    DropwizardExports dropwizardExports =
        new DropwizardExports(environment.metrics(), sampleBuilder);
    CollectorRegistry.defaultRegistry.register(dropwizardExports);

    environment.lifecycle().manage(onShutdown(CollectorRegistry.defaultRegistry::clear));
  }

  private List<MapperConfig> createMetricsMapperConfigs() {
    List<MapperConfig> mappers = new ArrayList<>();
    mappers.add(createMapperConfig("ch.qos.logback.core.*.*", "logback_appender", "name", "level"));
    mappers.add(createMapperConfig("jvm.gc.*.count", "jvm_gc_total", "step", DEPRECATED));
    mappers.add(createMapperConfig("jvm.gc.*.time", "jvm_gc_seconds", "step", DEPRECATED));
    mappers.add(
        createMapperConfig(
            "jvm.memory.pools.*.committed",
            "jvm_memory_pools_committed_bytes",
            "pool",
            DEPRECATED));
    mappers.add(
        createMapperConfig(
            "jvm.memory.pools.*.init", "jvm_memory_pools_init_bytes", "pool", DEPRECATED));
    mappers.add(
        createMapperConfig(
            "jvm.memory.pools.*.max", "jvm_memory_pools_max_bytes", "pool", DEPRECATED));
    mappers.add(
        createMapperConfig(
            "jvm.memory.pools.*.used", "jvm_memory_pools_used_bytes", "pool", DEPRECATED));
    mappers.add(
        createMapperConfig(
            "jvm.memory.pools.*.usage", "jvm_memory_pools_usage_ratio", "pool", DEPRECATED));
    mappers.add(
        createMapperConfig(
            "jvm.memory.pools.*.used-after-gc",
            "jvm_memory_pools_used_after_gc_bytes",
            "pool",
            DEPRECATED));
    mappers.add(
        createMapperConfig(
            "org.apache.http.conn.*.*.available-connections",
            APACHE_HTTP_CLIENT_CONNECTIONS,
            MANAGER,
            "name",
            new AbstractMap.SimpleImmutableEntry<>("state", "available"))); // NOSONAR
    mappers.add(
        createMapperConfig(
            "org.apache.http.conn.*.*.leased-connections",
            APACHE_HTTP_CLIENT_CONNECTIONS,
            MANAGER,
            "name",
            new AbstractMap.SimpleImmutableEntry<>("state", "leased")));
    mappers.add(
        createMapperConfig(
            "org.apache.http.conn.*.*.max-connections",
            APACHE_HTTP_CLIENT_CONNECTIONS,
            MANAGER,
            "name",
            new AbstractMap.SimpleImmutableEntry<>("state", "max")));
    mappers.add(
        createMapperConfig(
            "org.apache.http.conn.*.*.pending-connections",
            APACHE_HTTP_CLIENT_CONNECTIONS,
            MANAGER,
            "name",
            new AbstractMap.SimpleImmutableEntry<>("state", "pending")));
    mappers.add(
        createMapperConfig(
            "org.apache.hc.client5.http.classic.*.*.get-requests",
            APACHE_HTTP_CLIENT_REQUEST_DURATION_SECONDS,
            MANAGER,
            "name",
            new AbstractMap.SimpleImmutableEntry<>("method", "get"))); // NOSONAR
    mappers.add(
        createMapperConfig(
            "org.apache.hc.client5.http.classic.*.*.post-requests",
            APACHE_HTTP_CLIENT_REQUEST_DURATION_SECONDS,
            MANAGER,
            "name",
            new AbstractMap.SimpleImmutableEntry<>("method", "post")));
    mappers.add(
        createMapperConfig(
            "org.apache.hc.client5.http.classic.*.*.put-requests",
            APACHE_HTTP_CLIENT_REQUEST_DURATION_SECONDS,
            MANAGER,
            "name",
            new AbstractMap.SimpleImmutableEntry<>("method", "put")));
    mappers.add(
        createMapperConfig(
            "org.apache.hc.client5.http.classic.*.*.delete-requests",
            APACHE_HTTP_CLIENT_REQUEST_DURATION_SECONDS,
            MANAGER,
            "name",
            new AbstractMap.SimpleImmutableEntry<>("method", "delete")));
    mappers.add(
        createMapperConfig(
            "org.apache.hc.client5.http.classic.*.*.head-requests",
            APACHE_HTTP_CLIENT_REQUEST_DURATION_SECONDS,
            MANAGER,
            "name",
            new AbstractMap.SimpleImmutableEntry<>("method", "head")));
    mappers.add(
        createMapperConfig(
            "org.apache.hc.client5.http.classic.*.*.connect-requests",
            APACHE_HTTP_CLIENT_REQUEST_DURATION_SECONDS,
            MANAGER,
            "name",
            new AbstractMap.SimpleImmutableEntry<>("method", "connect")));
    mappers.add(
        createMapperConfig(
            "org.apache.hc.client5.http.classic.*.*.options-requests",
            APACHE_HTTP_CLIENT_REQUEST_DURATION_SECONDS,
            MANAGER,
            "name",
            new AbstractMap.SimpleImmutableEntry<>("method", "options")));
    mappers.add(
        createMapperConfig(
            "org.apache.hc.client5.http.classic.*.*.trace-requests",
            APACHE_HTTP_CLIENT_REQUEST_DURATION_SECONDS,
            MANAGER,
            "name",
            new AbstractMap.SimpleImmutableEntry<>("method", "trace")));
    mappers.add(
        createMapperConfig(
            "org.eclipse.jetty.server.*.*.connections", "jetty_connections", "factory", "port"));
    mappers.add(
        createMapperConfig(
            "org.eclipse.jetty.util.thread.*.*.jobs",
            "jetty_util_thread_jobs_count",
            "type",
            "pool"));
    mappers.add(
        createMapperConfig(
            "org.eclipse.jetty.util.thread.*.*.size",
            "jetty_util_thread_size_count",
            "type",
            "pool"));
    mappers.add(
        createMapperConfig(
            "org.eclipse.jetty.util.thread.*.*.utilization",
            "jetty_util_thread_utilization_ratio",
            "type",
            "pool"));
    mappers.add(
        createMapperConfig(
            "org.eclipse.jetty.util.thread.*.*.utilization-max",
            "jetty_util_thread_max_utilization_ratio",
            "type",
            "pool"));
    mappers.add(
        createMapperConfig(
            "io.dropwizard.jetty.*.active-dispatches",
            "jetty_handler_active_dispatches_total",
            "handler")); // NOSONAR
    mappers.add(
        createMapperConfig(
            "io.dropwizard.jetty.*.active-requests",
            "jetty_handler_active_requests_total",
            "handler"));
    mappers.add(
        createMapperConfig(
            "io.dropwizard.jetty.*.active-suspended",
            "jetty_handler_active_suspended_total",
            "handler"));
    mappers.add(
        createMapperConfig(
            "io.dropwizard.jetty.*.async-dispatches", "jetty_handler_async_dispatches", "handler"));
    mappers.add(
        createMapperConfig(
            "io.dropwizard.jetty.*.async-timeouts", "jetty_handler_async_timeouts", "handler"));
    return mappers;
  }

  private MapperConfig createMapperConfig(String match, String name, Object... labelNames) {
    MapperConfig config = new MapperConfig();
    config.setMatch(match);
    config.setName(name);
    Map<String, String> labels = new HashMap<>();
    for (int i = 0; i < labelNames.length; ++i) {
      Object labelName = labelNames[i];

      if (labelName instanceof Entry<?, ?> pair) {
        labels.put(pair.getKey().toString(), pair.getValue().toString());
      } else {
        labels.put(labelName.toString(), "${" + i + "}");
      }
    }
    config.setLabels(labels);
    return config;
  }

  private void registerMetricsServlet(AdminEnvironment environment) {
    // Prometheus Servlet registration
    ServletRegistration.Dynamic dynamic = environment.addServlet("metrics", MetricsServlet.class);
    dynamic.addMapping(METRICS_SERVLET_URL);
    LOG.info("Registered Prometheus metrics servlet at '{}'", METRICS_SERVLET_URL);
  }

  private void registerHealthCheckMetrics(Environment environment) {
    DropwizardHealthCheckMeters dropwizardHealthCheckMeters = new DropwizardHealthCheckMeters();
    environment.healthChecks().addListener(dropwizardHealthCheckMeters);
    environment.lifecycle().manage(dropwizardHealthCheckMeters);
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
    <C extends Configuration> FinalBuilder<C> withConfigurationProvider(
        @NotNull PrometheusConfigurationProvider<C> configurationProvider);

    <C extends Configuration> FinalBuilder<C> withDefaultPrometheusConfig();

    /**
     * @deprecated Deprecated way to get a PrometheusBundle. Use
     *     withoutConfigurationProvider().build() instead
     * @return PrometheusBundle with default configuration
     */
    @Deprecated(since = "8.2.0")
    PrometheusBundle<Configuration> build();
  }

  public interface FinalBuilder<C extends Configuration> {
    PrometheusBundle<C> build();
  }

  public static class Builder implements InitialBuilder {

    @Override
    public <C extends Configuration> FinalBuilder<C> withConfigurationProvider(
        PrometheusConfigurationProvider<C> configurationProvider) {
      return new ConfigurationProviderBuilder<>(configurationProvider);
    }

    @Override
    public <C extends Configuration> FinalBuilder<C> withDefaultPrometheusConfig() {
      return new ConfigurationProviderBuilder<>();
    }

    @Override
    public PrometheusBundle<Configuration> build() {
      // to be consistent in the workflow we call ConfigurationProviderBuilder and then build()
      return new ConfigurationProviderBuilder<>().build();
    }
  }

  public static class ConfigurationProviderBuilder<C extends Configuration>
      implements FinalBuilder<C> {
    private final PrometheusConfigurationProvider<C> prometheusConfigurationProvider;

    private ConfigurationProviderBuilder(
        PrometheusConfigurationProvider<C> prometheusConfigurationProvider) {
      this.prometheusConfigurationProvider = prometheusConfigurationProvider;
    }

    private ConfigurationProviderBuilder() {
      this.prometheusConfigurationProvider = c -> createDefaultPrometheusConfiguration();
    }

    private PrometheusConfiguration createDefaultPrometheusConfiguration() {
      PrometheusConfiguration configuration = new PrometheusConfiguration();
      configuration.setRequestPercentiles(List.of(0.25, 0.5, 0.75, 0.9, 0.95, 0.99));
      configuration.setRequestDigitsOfPrecision(3);
      return configuration;
    }

    @Override
    public PrometheusBundle<C> build() {
      return new PrometheusBundle<>(prometheusConfigurationProvider);
    }
  }
}
