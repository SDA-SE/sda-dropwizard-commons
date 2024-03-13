package org.sdase.commons.server.opentelemetry.autoconfig;

import java.util.HashMap;
import java.util.Map;
import org.sdase.commons.server.dropwizard.bundles.SystemPropertyAndEnvironmentLookup;

/**
 * This config source provider is used to define custom defaults that are relevant for sda services.
 * A complete list of system properties that can be overridden can be found in <a
 * href="https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md">Autoconfig
 * module.</a>
 */
public final class SdaConfigPropertyProvider {
  private SdaConfigPropertyProvider() {
    // avoid creating instances
  }

  private static final String JAEGER_SERVICE_NAME_ENV_VAR = "JAEGER_SERVICE_NAME";

  public static Map<String, String> getProperties() {
    Map<String, String> properties = new HashMap<>();

    // default jaeger collector gateway endpoint 14250
    properties.put("otel.exporter.otlp.endpoint", "http://jaeger-collector.jaeger:4317");
    // Disable exporting metrics
    properties.put("otel.metrics.exporter", "none");

    // 'jaeger' header formats are used by the deprecated libraries.
    // 'b3' is required to correlate traces with services instrumented by DW commons 2.x.x
    // 'tracecontext' 'baggage' formats are recommended defaults and used by most external systems.
    properties.put("otel.propagators", "jaeger,b3,tracecontext,baggage");

    // use environment legacy env variable defined by jaeger client by default
    String jaegerServiceName =
        new SystemPropertyAndEnvironmentLookup().lookup(JAEGER_SERVICE_NAME_ENV_VAR);
    if (jaegerServiceName != null && !jaegerServiceName.isEmpty()) {
      properties.put("otel.service.name", jaegerServiceName);
    }

    return properties;
  }
}
