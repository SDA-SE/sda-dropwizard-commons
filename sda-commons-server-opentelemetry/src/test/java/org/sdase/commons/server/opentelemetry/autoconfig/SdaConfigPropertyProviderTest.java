package org.sdase.commons.server.opentelemetry.autoconfig;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

class SdaConfigPropertyProviderTest {
  /**
   * A temporary test to verify that deprecated configuration options are still working. Could be
   * removed in the next major release.
   */
  @Test
  @SetSystemProperty(key = "JAEGER_SERVICE_NAME", value = "my-example-service")
  void shouldConsiderOldServiceName() {
    assertThat(SdaConfigPropertyProvider.getProperties())
        .containsEntry("otel.service.name", "my-example-service");
  }
}
