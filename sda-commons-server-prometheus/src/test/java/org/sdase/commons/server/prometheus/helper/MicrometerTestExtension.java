package org.sdase.commons.server.prometheus.helper;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import java.util.Set;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class MicrometerTestExtension implements AfterAllCallback {
  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    try {

      Set<MeterRegistry> registries = Metrics.globalRegistry.getRegistries();
      assertThat(registries).isEmpty();
    } catch (AssertionError e) {
      assertThat(e)
          .hasMessage(
              "Micrometer global registry should be zero after shutdown but was not. See MicrometerTestExtension.class");
    }
  }
}
