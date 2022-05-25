package org.sdase.commons.shared.otel.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

class SdaConfigPropertyProviderTest {
  @Test
  void shouldGetDefaultProperties() {
    Map<String, String> properties = new SdaConfigPropertyProvider().getProperties();
    assertThat(properties)
        .isNotEmpty()
        .contains(
            entry("otel.exporter.otlp.endpoint", "http://otel-collector-gateway.jaeger:4317"),
            entry("otel.propagators", "tracecontext,baggage,jaeger"))
        .doesNotContainKey("otel.service.name");
  }

  @Test
  @SetEnvironmentVariable(key = "JAEGER_SERVICE_NAME", value = "MyService")
  void shouldUseJaegerServiceName() {
    Map<String, String> properties = new SdaConfigPropertyProvider().getProperties();
    assertThat(properties).isNotEmpty().contains(entry("otel.service.name", "MyService"));
  }
}
