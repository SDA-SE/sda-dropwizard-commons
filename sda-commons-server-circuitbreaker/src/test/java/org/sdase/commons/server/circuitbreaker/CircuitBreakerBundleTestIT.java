package org.sdase.commons.server.circuitbreaker;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnIgnoredErrorEvent;
import io.micrometer.core.instrument.Metrics;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.prometheus.PrometheusBundle;

class CircuitBreakerBundleTestIT {
  @RegisterExtension
  @Order(0)
  static final WireMockExtension WIRE = new WireMockExtension.Builder().build();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<AppConfiguration> DW =
      new DropwizardAppExtension<>(
          TestApp.class, ResourceHelpers.resourceFilePath("test-config.yml"));

  @Test
  void shouldCreateCircuitBreaker() {
    TestApp app = DW.getApplication();
    var circuitBreakerBundle = app.getCircuitBreakerBundle();
    CircuitBreaker circuitBreaker =
        circuitBreakerBundle.createCircuitBreaker("create").withDefaultConfig().build();

    assertThat(circuitBreaker).isNotNull();
  }

  @Test
  void shouldHaveDefaultConfiguration() {
    TestApp app = DW.getApplication();
    var circuitBreakerBundle = app.getCircuitBreakerBundle();
    CircuitBreaker circuitBreaker =
        circuitBreakerBundle.createCircuitBreaker("default").withDefaultConfig().build();

    assertThat(circuitBreaker.getCircuitBreakerConfig())
        .extracting(
            CircuitBreakerConfig::getFailureRateThreshold,
            CircuitBreakerConfig::getSlidingWindowSize,
            CircuitBreakerConfig::getPermittedNumberOfCallsInHalfOpenState,
            config -> config.getWaitIntervalFunctionInOpenState().apply(1))
        .containsExactly(51.0f, 100, 10, Duration.of(1, MINUTES).toMillis());
    assertThat(
            circuitBreaker
                .getCircuitBreakerConfig()
                .isAutomaticTransitionFromOpenToHalfOpenEnabled())
        .isTrue();
  }

  @Test
  void shouldHaveCustomConfiguration() {
    TestApp app = DW.getApplication();
    var circuitBreakerBundle = app.getCircuitBreakerBundle();
    CircuitBreaker circuitBreaker =
        circuitBreakerBundle
            .createCircuitBreaker("custom")
            .withCustomConfig(
                new CircuitBreakerConfiguration()
                    .setFailureRateThreshold(75.0f)
                    .setRingBufferSizeInClosedState(50)
                    .setRingBufferSizeInHalfOpenState(5)
                    .setWaitDurationInOpenState(Duration.of(30, SECONDS)))
            .build();

    assertThat(circuitBreaker.getCircuitBreakerConfig())
        .extracting(
            CircuitBreakerConfig::getFailureRateThreshold,
            CircuitBreakerConfig::getSlidingWindowSize,
            CircuitBreakerConfig::getPermittedNumberOfCallsInHalfOpenState,
            config -> config.getWaitIntervalFunctionInOpenState().apply(1))
        .containsExactly(75.0f, 50, 5, Duration.of(30, SECONDS).toMillis());
    assertThat(
            circuitBreaker
                .getCircuitBreakerConfig()
                .isAutomaticTransitionFromOpenToHalfOpenEnabled())
        .isTrue();
  }

  @Test
  void shouldApplyIgnoredExceptions() {
    TestApp app = DW.getApplication();
    var circuitBreakerBundle = app.getCircuitBreakerBundle();
    CircuitBreaker circuitBreaker =
        circuitBreakerBundle
            .createCircuitBreaker("default")
            .withDefaultConfig()
            .ignoreExceptions(IllegalStateException.class)
            .build();
    List<CircuitBreakerOnIgnoredErrorEvent> ignoredErrors = new ArrayList<>();
    circuitBreaker.getEventPublisher().onIgnoredError(ignoredErrors::add);

    try {
      circuitBreaker.executeSupplier(
          () -> {
            throw new IllegalStateException();
          });
    } catch (Exception ex) {
      // Handle exception
    }

    assertThat(ignoredErrors).isNotEmpty();
  }

  @Test
  void shouldWrapTarget() {
    TestApp app = DW.getApplication();
    var circuitBreakerBundle = app.getCircuitBreakerBundle();
    Simple target =
        circuitBreakerBundle
            .createCircuitBreaker("default")
            .withDefaultConfig()
            .wrap(new SimpleImpl());

    assertThat(target.check()).isEqualTo(42);
  }

  @Test
  void shouldProvideMetrics() {
    TestApp app = DW.getApplication();
    var circuitBreakerBundle = app.getCircuitBreakerBundle();
    CircuitBreaker circuitBreaker =
        circuitBreakerBundle.createCircuitBreaker("metrics").withDefaultConfig().build();
    circuitBreaker.executeSupplier(() -> true);

    var meters =
        Metrics.globalRegistry.getRegistries().stream()
            .flatMap(registry -> registry.getMeters().stream())
            .filter(meter -> meter.getId().getName().startsWith("resilience4j.circuitbreaker"))
            .toList();
    assertThat(meters).isNotEmpty();
  }

  public static class AppConfiguration extends Configuration {
    private CircuitBreakerConfiguration circuitBreaker;

    public CircuitBreakerConfiguration getCircuitBreaker() {
      return circuitBreaker;
    }

    public AppConfiguration setCircuitBreaker(CircuitBreakerConfiguration circuitBreaker) {
      this.circuitBreaker = circuitBreaker;
      return this;
    }
  }

  public static class TestApp extends Application<AppConfiguration> {
    private final CircuitBreakerBundle<AppConfiguration> circuitBreakerBundle =
        CircuitBreakerBundle.builder()
            .<AppConfiguration>withCustomConfig(
                new CircuitBreakerConfiguration().setFailureRateThreshold(51.0f))
            .build();

    @Override
    public void initialize(Bootstrap<AppConfiguration> bootstrap) {
      bootstrap.addBundle(PrometheusBundle.builder().build());
      bootstrap.addBundle(circuitBreakerBundle);
    }

    @Override
    public void run(AppConfiguration configuration, Environment environment) {
      // nothing to run
    }

    public CircuitBreakerBundle<AppConfiguration> getCircuitBreakerBundle() {
      return circuitBreakerBundle;
    }
  }

  interface Simple {
    int check();
  }

  static class SimpleImpl implements Simple {

    public int check() {
      return 42;
    }
  }
}
