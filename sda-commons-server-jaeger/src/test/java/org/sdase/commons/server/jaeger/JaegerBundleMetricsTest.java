package org.sdase.commons.server.jaeger;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Configuration;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.jaeger.test.TraceTestApp;

public class JaegerBundleMetricsTest {

  @ClassRule
  public static final DropwizardAppRule<Configuration> DW =
      new DropwizardAppRule<>(TraceTestApp.class, resourceFilePath("test-config.yaml"));

  @Test
  public void shouldHavePrometheusMetrics() {
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
