package org.sdase.commons.shared.otel.agent;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.internal.StringUtils;
import io.opentelemetry.javaagent.extension.config.ConfigPropertySource;
import java.util.HashMap;
import java.util.Map;

/**
 * This config source provider is used to define custom defaults that are relevant for sda services.
 * A complete list of system properties that can be overridden can be found in <a
 * href="https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md">Autoconfig
 * module.</a>
 */
@AutoService(ConfigPropertySource.class)
public final class SdaConfigPropertyProvider implements ConfigPropertySource {
  private static final String JAEGER_SERVER_NAME_ENV_VAR = "JAEGER_SERVICE_NAME";

  @Override
  public Map<String, String> getProperties() {
    Map<String, String> properties = new HashMap<>();

    // default otel collector gateway endpoint 4317 used for grpc (TBD)
    properties.put("otel.exporter.otlp.endpoint", "http://otel-collector-gateway:4317");

    // 'jaeger' header formats are used by the deprecated libraries
    // 'tracecontext' 'baggage' formats are recommended defaults and used by most external
    // distributed systems.
    properties.put("otel.propagators", "tracecontext,baggage,jaeger");

    // use environment legacy env variable defined by jaeger client by default
    String jaegerServiceName = System.getenv(JAEGER_SERVER_NAME_ENV_VAR);
    if (!StringUtils.isNullOrEmpty(jaegerServiceName)) {
      properties.put("otel.service.name", jaegerServiceName);
    }
    return properties;
  }
}
