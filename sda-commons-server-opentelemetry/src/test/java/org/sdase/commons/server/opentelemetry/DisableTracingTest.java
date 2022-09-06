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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "MAIN_THREAD_CHECK_ENABLED", value = "false")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DisableTracingTest {
  private DropwizardTestSupport<Configuration> DW;

  @AfterAll
  static void afterAll() {
    GlobalOpenTelemetry.resetForTest();
  }

  @BeforeEach
  void setUp() throws Exception {
    DW = new DropwizardTestSupport<>(TestApp.class, null, randomPorts());
    DW.before();
  }

  @Test
  @Order(0)
  @SetEnvironmentVariable(key = "OTEL_EXPERIMENTAL_SDK_ENABLED", value = "false")
  void shouldDisableTracing() {
    assertThat(System.getenv("OTEL_EXPERIMENTAL_SDK_ENABLED")).isEqualTo("false");
    assertThat(GlobalOpenTelemetry.get().getPropagators().getTextMapPropagator().fields())
        .isEmpty();
  }

  @Test
  @Order(1)
  @SetEnvironmentVariable(key = "JAEGER_SAMPLER_TYPE", value = "const")
  @SetEnvironmentVariable(key = "JAEGER_SAMPLER_PARAM", value = "0")
  void shouldDisableTracingWithLegacyParams() {
    assertThat(System.getenv("JAEGER_SAMPLER_TYPE")).isEqualTo("const");
    assertThat(System.getenv("JAEGER_SAMPLER_PARAM")).isEqualTo("0");
    OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();

    assertThat(openTelemetry.getPropagators().getTextMapPropagator().fields()).isEmpty();
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
