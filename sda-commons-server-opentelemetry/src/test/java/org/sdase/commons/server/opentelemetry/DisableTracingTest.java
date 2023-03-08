package org.sdase.commons.server.opentelemetry;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.DropwizardTestSupport;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import java.io.Closeable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

class DisableTracingTest {

  @BeforeEach
  @AfterEach
  void cleanupTracer() {
    GlobalOpenTelemetry.resetForTest();
  }

  @Test
  @SetSystemProperty(key = "TRACING_DISABLED", value = "true")
  void shouldDisableTracing() throws Exception {
    try (var ignored = startApp()) {
      assertThat(System.getProperty("TRACING_DISABLED")).isEqualTo("true");
      assertThat(GlobalOpenTelemetry.get().getPropagators().getTextMapPropagator().fields())
          .isEmpty();
      assertThat(GlobalOpenTelemetry.get()).extracting("delegate").isSameAs(OpenTelemetry.noop());
    }
  }

  @Test
  @SetSystemProperty(key = "JAEGER_SAMPLER_TYPE", value = "const")
  void shouldNotDisableTracingJaegerSampleType() throws Exception {
    try (var ignored = startApp()) {
      assertThat(System.getProperty("TRACING_DISABLED")).isNull();
      assertThat(GlobalOpenTelemetry.get())
          .extracting("delegate")
          .isNotSameAs(OpenTelemetry.noop());
    }
  }

  @Test
  @SetSystemProperty(key = "JAEGER_SAMPLER_PARAM", value = "0")
  void shouldNotDisableTracingJaegerSampleName() throws Exception {
    try (var ignored = startApp()) {
      assertThat(System.getProperty("TRACING_DISABLED")).isNull();
      assertThat(GlobalOpenTelemetry.get())
          .extracting("delegate")
          .isNotSameAs(OpenTelemetry.noop());
    }
  }

  @Test
  @SetSystemProperty(key = "JAEGER_SAMPLER_TYPE", value = "const")
  @SetSystemProperty(key = "JAEGER_SAMPLER_PARAM", value = "0")
  void shouldDisableTracingWithLegacyParams() throws Exception {
    try (var ignored = startApp()) {
      assertThat(System.getProperty("JAEGER_SAMPLER_TYPE")).isEqualTo("const");
      assertThat(System.getProperty("JAEGER_SAMPLER_PARAM")).isEqualTo("0");
      assertThat(GlobalOpenTelemetry.get().getPropagators().getTextMapPropagator().fields())
          .isEmpty();
      assertThat(GlobalOpenTelemetry.get()).extracting("delegate").isSameAs(OpenTelemetry.noop());
    }
  }

  private Closeable startApp() throws Exception {
    DropwizardTestSupport<Configuration> DW =
        new DropwizardTestSupport<>(TestApp.class, null, randomPorts());
    DW.before();
    return DW::after;
  }

  public static class TestApp extends Application<Configuration> {

    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) {
      // use the autoConfigured module
      bootstrap.addBundle(
          OpenTelemetryBundle.builder().withAutoConfiguredTelemetryInstance().build());
    }

    @Override
    public void run(Configuration configuration, Environment environment) {
      // do nothing
    }
  }
}
