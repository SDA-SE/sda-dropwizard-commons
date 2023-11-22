package org.sdase.commons.server.opentelemetry;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.opentelemetry.api.GlobalOpenTelemetry;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AutoConfigurationTest {

  @RegisterExtension
  public static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(TraceTestApp.class, null, randomPorts());

  @Test
  @Order(2)
  void shouldUseDefaults() {
    assertThat(GlobalOpenTelemetry.get().getPropagators().getTextMapPropagator().fields())
        .isNotEmpty()
        .contains("traceparent", "baggage", "uber-trace-id");
  }

  @Test
  @StdIo
  @Order(3)
  @SuppressWarnings("JUnitMalformedDeclaration") // inspection not expecting parameter for StdIo
  void shouldUseEnvironmentVariablesForConfiguration(StdOut out) {
    assertThat(System.getenv("OTEL_TRACES_EXPORTER")).isEqualTo("logging");

    Response r = createClient().path("base/respond/test").request().get();

    r.readEntity(String.class);

    assertThat(r.getStatus()).isEqualTo(SC_OK);

    // assert the logging exporter is used
    await()
        .untilAsserted(
            () ->
                assertThat(out.capturedLines())
                    .isNotEmpty()
                    .anyMatch(
                        l ->
                            l.contains("[tracer: sda-commons.servlet:]")
                                && l.contains(
                                    "io.opentelemetry.exporter.logging.LoggingSpanExporter: 'GET /base/respond/{value}'")));
  }

  private WebTarget createClient() {
    return DW.client().target("http://localhost:" + DW.getLocalPort());
  }

  public static class TraceTestApp extends Application<Configuration> {

    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) {
      // use the autoConfigured module
      bootstrap.addBundle(
          OpenTelemetryBundle.builder().withAutoConfiguredTelemetryInstance().build());
    }

    @Override
    public void run(Configuration configuration, Environment environment) {
      environment.jersey().register(new TestApi());
    }
  }
}
