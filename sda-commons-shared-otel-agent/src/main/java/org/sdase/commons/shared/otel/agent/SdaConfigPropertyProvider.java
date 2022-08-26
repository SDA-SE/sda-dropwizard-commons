package org.sdase.commons.shared.otel.agent;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.config.ConfigPropertySource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This config source provider is used to define custom defaults that are relevant for sda services.
 * A complete list of system properties that can be overridden can be found in <a
 * href="https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md">Autoconfig
 * module.</a>
 */
@AutoService(ConfigPropertySource.class)
public final class SdaConfigPropertyProvider implements ConfigPropertySource {
  private static final String JAEGER_SERVICE_NAME_ENV_VAR = "JAEGER_SERVICE_NAME";

  @Override
  public Map<String, String> getProperties() {
    Map<String, String> properties = new HashMap<>();

    // default otel collector gateway endpoint 4317 used for grpc
    properties.put("otel.exporter.otlp.endpoint", "http://otel-collector-gateway.jaeger:4317");
    // Disable exporting metrics
    properties.put("otel.metrics.exporter", "none");

    // 'jaeger' header formats are used by the deprecated libraries.
    // 'tracecontext' 'baggage' formats are recommended defaults and used by most external systems.
    properties.put("otel.propagators", "tracecontext,baggage,jaeger");

    // use environment legacy env variable defined by jaeger client by default
    String jaegerServiceName = System.getenv(JAEGER_SERVICE_NAME_ENV_VAR);
    if (jaegerServiceName != null && !jaegerServiceName.isEmpty()) {
      properties.put("otel.service.name", jaegerServiceName);
    }

    // include only some instrumentations
    properties.put("otel.instrumentation.common.default-enabled", "false");
    getEnabledInstrumentationModules().forEach(lib -> properties.put(lib, "true"));

    return properties;
  }

  private List<String> getEnabledInstrumentationModules() {
    return Arrays.asList(
        "otel.instrumentation.jaxrs.enabled",
        "otel.instrumentation.jersey.enabled",
        "otel.instrumentation.mongo.enabled",
        "otel.instrumentation.apache-httpclient.enabled",
        "otel.instrumentation.kafka.enabled",
        "otel.instrumentation.aws-sdk.enabled");
  }
}
