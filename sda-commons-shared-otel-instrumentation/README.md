# SDA Commons Shared OpenTelemetry Instrumentation

This module is responsible for loading the OpenTelemetry [Java agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation) dynamically to the current application.
The agent provides automatic instrumentation for various popular libraries and frameworks [full List](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/supported-libraries.md#libraries--frameworks).
The agent can be configured to export the captured telemetry data produced by these libraries in different formats.

# Docs
An extensive documentation can be found in [OpenTelemetry Java documentation](https://opentelemetry.io/docs/instrumentation/java/). 

# Usage

This bundle requires the [openTelemetry agent](https://mvnrepository.com/artifact/io.opentelemetry.javaagent/opentelemetry-javaagent) to be added as a runtime dependency to the application.
Sda-commons already provides an extension of the agent [sda-commons-shared-otel-agent](../sda-commons-shared-otel-agent) that can be used.

```groovy
  api project(':sda-commons-shared-otel-instrumentation')
  runtimeOnly project(':sda-commons-shared-otel-agent')
```

Then the bundle is ready to be added to the application.

```java
public class MyApp extends Application<Configuration> {
  @Override
  public void run(Configuration configuration, Environment environment) {}

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    // ... other bundles  
    bootstrap.addBundle(InstrumentationBundle.builder().build());
  }
}
```

If the application is already using the [starter bundle](../sda-commons-server-starter), no changes are needed.
The agent is loaded and ready to be configured with environment variables.

> **_NOTE:_** If the application is using the starter bundle, which is already using the [sda-commons-shared-otel-agent](../sda-commons-shared-otel-agent), and it is required to use another vendor extension, the `sda-commons-shared-otel-agent` module must be excluded from the starter bundle.

# Agent configuration

The agent is highly configurable! Many aspects of the agent's behavior can be configured for your needs, such as exporter choice, exporter config (like where data is sent), trace context propagation headers...
The configuration can be done either with environment variables or system properties. Please note that system properties will have a higher priority over environment variables.

A full list of the configurable properties can be found in the [autoconfigure module](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure#exporters).

It is recommended to use same configuration across all services in the same domain, to ensure the right context propagation, and export traces to the same monitoring backend.

## Service Name

The service name is used to identify the source (the sender) of the received telemetry data in the monitoring backend. Therefore it's important that every service has a unique name.
To provide the application with a custom name for each deployment, the `OTEL_SERVICE_NAME` environment variable must be used.

## Basic configuration

By default, the agent exports traces in the otlp format, to an [openTelemetry collector](https://opentelemetry.io/docs/collector/). The most basic configuration would be to provide the service name and a collector endpoint.
- `OTEL_SERVICE_NAME=my-service`
- `OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-gateway-host:4317`.

Or in case if the collector is deployed as a sidecar:
- `OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317`

## Using Jaeger exporter

To exports traces in Jaeger format, to a [Jaeger collector](https://hub.docker.com/r/jaegertracing/jaeger-collector/). This can be very handy for less local setup overhead, where the [All in one Jaeger image](https://www.jaegertracing.io/docs/1.6/getting-started/#all-in-one-docker-image) can be enough.
- `OTEL_SERVICE_NAME=my-service`
- `OTEL_EXPORTER_JAEGER_ENDPOINT=http://jaeger-collector-host:14250`

> **_NOTE:_** It is recommended to not use this option in production even if the used monitoring backend is Jaeger. It is better to export the telemetry data in the _OTLP_ format to a collector, and configure it to handle the transformation.

# Disabling instrumentation

In order to disable tracing in the applications that are using this bundle, or the [starter bundle](../sda-commons-server-starter), the environment variable `OTEL_JAVAAGENT_ENABLED=false` can be used.
Please note that setting `OTEL_JAVAAGENT_ENABLED` to false will force the bundle to completely skip loading the agent Jar into the JVM, so manual instrumentation like added in the [sda-commons-shared-otel-tracing-example](../sda-commons-shared-otel-tracing-example) will not be possible anymore.
In this case manual tracing must be done differently (see other options for [manual instrumentation](https://opentelemetry.io/docs/instrumentation/java/manual/)).

# Enable manual instrumentation only

In order to suppress all auto instrumentation but still want to load the agent the option `OTEL_INSTRUMENTATION_COMMON_DEFAULT_ENABLED=false`.
This option can be used to still have support for manual instrumentation with combination of:
- `OTEL_INSTRUMENTATION_OPENTELEMETRY_API_ENABLED=true` to still send spans created manually using the openTelemetry api (`tracer.spanBuilder("my span").startSpan()`).
- `OTEL_INSTRUMENTATION_OPENTELEMETRY_ANNOTATIONS_ENABLED=true` to still export spans created manually with the `@WithSpan` annotation.