package org.sdase.commons.server.jaeger;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.jaeger.test.TraceTestApp;

class JaegerBundleMetricsTest {

  @RegisterExtension
  public static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(TraceTestApp.class, null, randomPorts());

  @Test
  void shouldHavePrometheusMetrics() {
    // Warmup jaeger first by performing a request:
    DW.client().target("http://localhost:" + DW.getLocalPort()).request().get();

    String metrics =
        DW.client()
            .target("http://localhost:" + DW.getAdminPort())
            .path("metrics/prometheus")
            .request()
            .get()
            .readEntity(String.class);

    assertThat(metrics).contains("jaeger_tracer_started_spans");
  }
}
