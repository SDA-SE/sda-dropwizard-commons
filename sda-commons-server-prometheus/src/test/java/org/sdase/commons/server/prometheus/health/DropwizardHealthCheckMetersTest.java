package org.sdase.commons.server.prometheus.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import io.micrometer.core.instrument.Gauge;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DropwizardHealthCheckMetersTest {

  HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
  DropwizardHealthCheckMeters dropwizardHealthCheckMeters = new DropwizardHealthCheckMeters();

  @BeforeEach
  void setUp() {
    healthCheckRegistry.addListener(dropwizardHealthCheckMeters);
  }

  @Test
  void shouldAddGauges() {
    healthCheckRegistry.register("test-healthy", healthy());
    healthCheckRegistry.register("test-unhealthy", unhealthy());

    assertThatMeters().hasSize(2).allSatisfy(e -> assertThat(e).isInstanceOf(Gauge.class));
  }

  @Test
  void shouldRemoveGauges() {
    healthCheckRegistry.register("test-healthy", healthy());
    healthCheckRegistry.register("test-unhealthy", unhealthy());
    assertThatMeters().hasSize(2);

    healthCheckRegistry.unregister("test-unhealthy");
    assertThatMeters().hasSize(1);
  }

  AbstractListAssert<?, List<?>, Object, ObjectAssert<Object>> assertThatMeters() {
    return assertThat(dropwizardHealthCheckMeters)
        .extracting("healthCheckMeters")
        .asInstanceOf(MAP)
        .extracting(m -> new ArrayList<>(m.values()))
        .asList();
  }

  HealthCheck healthy() {
    return new HealthCheck() {
      @Override
      protected Result check() {
        return Result.healthy("Yeah!");
      }
    };
  }

  HealthCheck unhealthy() {
    return new HealthCheck() {
      @Override
      protected Result check() {
        return Result.unhealthy("Oh no.");
      }
    };
  }
}
