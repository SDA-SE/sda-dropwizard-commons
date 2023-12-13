package org.sdase.commons.server.prometheus.health;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistryListener;
import io.dropwizard.lifecycle.Managed;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.sdase.commons.server.dropwizard.lifecycle.ManagedShutdownListener;

/**
 * Registers Dropwizard {@link HealthCheck}s as Micrometer {@link Gauge}s to monitor health checks
 * over time.
 *
 * <p>The health check metrics {@value #HEALTH_CHECK_STATUS_METRIC} are propagated to the {@link
 * Metrics#globalRegistry}.
 *
 * <p>An instance of this class must be registered as {@link
 * com.codahale.metrics.health.HealthCheckRegistry#addListener(HealthCheckRegistryListener)} to
 * collect health checks and properly {@linkplain
 * io.dropwizard.lifecycle.setup.LifecycleEnvironment#manage(Managed) managed} to be closed on
 * shutdown.
 */
public class DropwizardHealthCheckMeters
    implements HealthCheckRegistryListener, ManagedShutdownListener {

  static final String HEALTH_CHECK_STATUS_METRIC = "healthcheck_status";

  private final Map<String, Meter> healthCheckMeters = new ConcurrentHashMap<>();

  @Override
  public void onHealthCheckAdded(String name, HealthCheck healthCheck) {
    Gauge gauge =
        Gauge.builder(HEALTH_CHECK_STATUS_METRIC, healthCheck, this::createValue)
            .tag("name", name)
            .description("Status of a Health Check (1: healthy, 0: unhealthy)")
            .register(Metrics.globalRegistry);
    healthCheckMeters.put(name, gauge);
    gauge.measure();
  }

  @Override
  public void onHealthCheckRemoved(String name, HealthCheck healthCheck) {
    Meter gaugeToRemove = healthCheckMeters.remove(name);
    if (gaugeToRemove != null) {
      Metrics.globalRegistry.remove(gaugeToRemove);
      gaugeToRemove.close();
    }
  }

  private double createValue(HealthCheck healthCheck) {
    HealthCheck.Result result = healthCheck.execute();
    return (result != null && result.isHealthy()) ? 1.0d : 0.0d;
  }

  @Override
  public void onShutdown() {
    new HashSet<>(healthCheckMeters.keySet()).forEach(name -> onHealthCheckRemoved(name, null));
  }
}
