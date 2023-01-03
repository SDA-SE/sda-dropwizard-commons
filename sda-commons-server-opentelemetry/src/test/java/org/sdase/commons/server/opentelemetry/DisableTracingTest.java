package org.sdase.commons.server.opentelemetry;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.DropwizardTestSupport;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junitpioneer.jupiter.SetSystemProperty;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DisableTracingTest {

  @AfterEach
  void tearDown() {
    GlobalOpenTelemetry.resetForTest();
  }

  @BeforeEach
  void setUp() throws Exception {
    DropwizardTestSupport<Configuration> DW =
        new DropwizardTestSupport<>(TestApp.class, null, randomPorts());
    DW.before();
  }

  @Test
  @Order(0)
  @SetSystemProperty(key = "TRACING_DISABLED", value = "true")
  void shouldDisableTracing() {
    assertThat(System.getProperty("TRACING_DISABLED")).isEqualTo("true");
    assertThat(GlobalOpenTelemetry.get().getPropagators().getTextMapPropagator().fields())
        .isEmpty();
  }

  @Test
  @Order(1)
  @SetSystemProperty(key = "JAEGER_SAMPLER_TYPE", value = "const")
  @SetSystemProperty(key = "JAEGER_SAMPLER_PARAM", value = "0")
  void shouldDisableTracingWithLegacyParams() {
    assertThat(System.getProperty("JAEGER_SAMPLER_TYPE")).isEqualTo("const");
    assertThat(System.getProperty("JAEGER_SAMPLER_PARAM")).isEqualTo("0");
    assertThat(GlobalOpenTelemetry.get().getPropagators().getTextMapPropagator().fields())
        .isEmpty();
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
