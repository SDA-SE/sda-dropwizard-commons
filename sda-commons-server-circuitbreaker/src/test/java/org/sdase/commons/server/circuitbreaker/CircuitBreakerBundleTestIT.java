package org.sdase.commons.server.circuitbreaker;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnIgnoredErrorEvent;
import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.circuitbreaker.CircuitBreakerWrapperHelperTest.Simple;
import org.sdase.commons.server.prometheus.PrometheusBundle;
import org.sdase.commons.server.testing.LazyRule;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

public class CircuitBreakerBundleTestIT {
   private static final WireMockClassRule WIRE = new WireMockClassRule(wireMockConfig().dynamicPort());

   private static final LazyRule<DropwizardAppRule<AppConfiguration>> DW = new LazyRule<>(
         () -> new DropwizardAppRule<>(TestApp.class, resourceFilePath("test-config.yml")));

   @ClassRule
   public static final RuleChain CHAIN = RuleChain.outerRule(WIRE).around(DW);

   @Test
   public void shouldCreateCircuitBreaker() {
      TestApp app = DW.getRule().getApplication();
      CircuitBreakerBundle circuitBreakerBundle = app.getCircuitBreakerBundle();
      CircuitBreaker circuitBreaker = circuitBreakerBundle.createCircuitBreaker("create").withDefaultConfig().build();

      assertThat(circuitBreaker).isNotNull();
   }

   @Test
   public void shouldHaveDefaultConfiguration() {
      TestApp app = DW.getRule().getApplication();
      CircuitBreakerBundle circuitBreakerBundle = app.getCircuitBreakerBundle();
      CircuitBreaker circuitBreaker = circuitBreakerBundle.createCircuitBreaker("default").withDefaultConfig().build();

      assertThat(circuitBreaker.getCircuitBreakerConfig())
            .extracting(CircuitBreakerConfig::getFailureRateThreshold,
                  CircuitBreakerConfig::getRingBufferSizeInClosedState,
                  CircuitBreakerConfig::getRingBufferSizeInHalfOpenState,
                  CircuitBreakerConfig::getWaitDurationInOpenState)
            .containsExactly(51.0f, 100, 10, Duration.of(1, MINUTES));
      assertThat(circuitBreaker.getCircuitBreakerConfig().isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
   }

   @Test
   public void shouldHaveCustomConfiguration() {
      TestApp app = DW.getRule().getApplication();
      CircuitBreakerBundle circuitBreakerBundle = app.getCircuitBreakerBundle();
      CircuitBreaker circuitBreaker = circuitBreakerBundle
            .createCircuitBreaker("custom")
            .withCustomConfig(new CircuitBreakerConfiguration()
                  .setFailureRateThreshold(75.0f)
                  .setRingBufferSizeInClosedState(50)
                  .setRingBufferSizeInHalfOpenState(5)
                  .setWaitDurationInOpenState(Duration.of(30, SECONDS)))
            .build();

      assertThat(circuitBreaker.getCircuitBreakerConfig())
            .extracting(CircuitBreakerConfig::getFailureRateThreshold,
                  CircuitBreakerConfig::getRingBufferSizeInClosedState,
                  CircuitBreakerConfig::getRingBufferSizeInHalfOpenState,
                  CircuitBreakerConfig::getWaitDurationInOpenState)
            .containsExactly(75.0f, 50, 5, Duration.of(30, SECONDS));
      assertThat(circuitBreaker.getCircuitBreakerConfig().isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
   }

   @Test
   public void shouldApplyIgnoredExceptions() {
      TestApp app = DW.getRule().getApplication();
      CircuitBreakerBundle circuitBreakerBundle = app.getCircuitBreakerBundle();
      CircuitBreaker circuitBreaker = circuitBreakerBundle
            .createCircuitBreaker("default")
            .withDefaultConfig()
            .ignoreExceptions(IllegalStateException.class)
            .build();
      List<CircuitBreakerOnIgnoredErrorEvent> ignoredErrors = new ArrayList<>();
      circuitBreaker.getEventPublisher().onIgnoredError(ignoredErrors::add);

      try {
         circuitBreaker.executeSupplier(() -> {
            throw new IllegalStateException();
         });
      } catch (Exception ex) {
         // Handle exception
      }

      assertThat(ignoredErrors).isNotEmpty();
   }

   @Test
   public void shouldWrapTarget() {
      TestApp app = DW.getRule().getApplication();
      CircuitBreakerBundle circuitBreakerBundle = app.getCircuitBreakerBundle();
      Simple target = circuitBreakerBundle.createCircuitBreaker("default").withDefaultConfig().wrap(new SimpleImpl());

      assertThat(target.check()).isEqualTo(42);
   }

   @Test
   public void shouldProvideMetrics() {
      TestApp app = DW.getRule().getApplication();
      CircuitBreakerBundle circuitBreakerBundle = app.getCircuitBreakerBundle();
      CircuitBreaker circuitBreaker = circuitBreakerBundle.createCircuitBreaker("metrics").withDefaultConfig().build();
      circuitBreaker.executeSupplier(() -> true);

      String metrics = DW
            .getRule()
            .client()
            .target("http://localhost:" + DW.getRule().getAdminPort())
            .path("metrics/prometheus")
            .request()
            .get()
            .readEntity(String.class);

      assertThat(metrics).contains("resilience4j_circuitbreaker_state{");
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
      private final CircuitBreakerBundle<AppConfiguration> circuitBreakerBundle = CircuitBreakerBundle
            .builder()
            .<AppConfiguration>withCustomConfig(new CircuitBreakerConfiguration().setFailureRateThreshold(51.0f))
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

      public CircuitBreakerBundle getCircuitBreakerBundle() {
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
