package org.sdase.commons.shared.otel.tracing;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Configuration;
import io.dropwizard.testing.DropwizardTestSupport;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.Test;

public class TracingBundleIT {
  @Test
  public void shouldCreateAnSdkInstance() throws Exception {
    DropwizardTestSupport<Configuration> DW =
        new DropwizardTestSupport<>(TestApp.class, null, randomPorts());
    DW.before();
    OpenTelemetry openTelemetry =
        DW.<TestApp>getApplication().getTracingBundle().getOpenTelemetry();
    assertThat(openTelemetry).isNotNull();
  }
}
