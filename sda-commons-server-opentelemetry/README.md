# SDA Commons Server OpenTelemetry

This bundle is responsible for loading the creating an OpenTelemetry Sdk instance and registering it as a global instance ready to use everywhere in dependent applications.
The module also creating server traces to insure proper context propagation.

## Docs
An extensive documentation can be found in [OpenTelemetry Java documentation](https://opentelemetry.io/docs/instrumentation/java/).

## Usage

The bundle must be initialized before other bundles in the dependent applications, as it is responsible for initializing the openTelemetry Sdk and registering the created instance as global, so that dependent bundles can use it.

```groovy
  api project(':sda-commons-server-opentelemetry')
```

Then the bundle is ready to be added to the application.

```java
public class MyApp extends Application<Configuration> {
  @Override
  public void run(Configuration configuration, Environment environment) {}

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    // use the autoConfigured module
    bootstrap.addBundle(
        OpenTelemetryBundle.builder().withAutoConfiguredTelemetryInstance().build());
    // ... other bundles  
  }
}
```

If the application is already using the [starter bundle](../sda-commons-starter), no changes are needed.
The module is already added and configured with environment variables.

> **_NOTE:_** Except for [starter bundle](../sda-commons-starter), no other bundle must depend on this one. The bundle registers a global Telemetry instance that is used all across the application has a safety mechanism to prevent registering a new instance twice.
> 
## Configuration

The OpenTelemetry sdk is highly configurable! Many aspects of it's behavior can be configured for your needs, such as exporter choice, exporter config (like where data is sent), trace context propagation headers...
The configuration can be done either with environment variables or system properties. Please note that system properties will have a higher priority over environment variables.

A full list of the configurable properties can be found in the [autoconfigure module](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure#exporters).

It is recommended to use same configuration across all services in the same domain, to ensure the right context propagation, and export traces to the same monitoring backend.

### Service Name

The service name is used to identify the source (the sender) of the received telemetry data in the monitoring backend. Therefore it's important that every service has a unique name.
To provide the application with a custom name for each deployment, the `OTEL_SERVICE_NAME` environment variable must be used.

### Basic configuration

By default, the module exports traces in the otlp format, to an [openTelemetry collector](https://opentelemetry.io/docs/collector/). The most basic configuration would be to provide the service name and a collector endpoint.
- `OTEL_SERVICE_NAME=my-service`
- `OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-gateway-host:4317`.

Or in case if the collector is deployed as a sidecar:
- `OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317`

### Using Jaeger exporter

To export traces in Jaeger format, to a [Jaeger collector](https://hub.docker.com/r/jaegertracing/jaeger-collector/). This can be very handy for less local setup overhead, where the [All in one Jaeger image](https://www.jaegertracing.io/docs/1.6/getting-started/#all-in-one-docker-image) can be enough.
- `OTEL_SERVICE_NAME=my-service`
- `OTEL_TRACES_EXPORTER=jaeger`
- `OTEL_EXPORTER_JAEGER_ENDPOINT=http://jaeger-collector-host:14250`

> **_NOTE:_** It is recommended to not use this option in production even if the used monitoring backend is Jaeger. It is better to export the telemetry data in the _OTLP_ format to a collector, and configure it to handle the transformation.

## Disable Tracing

In order to disable tracing in the applications that are using this bundle, or the [starter bundle](../sda-commons-starter), the environment variable `OTEL_DISABLED=true` can be used.
Setting `OTEL_DISABLED` to false will force the instrumented modules provided by sda-commons to use a no-op instance.

## Manual instrumentation

Sda commons already offers the necessary instrumentation for the server and some clients, to insure a better overview about service to service interaction.
It is advised to avoid unnecessary tracing for interaction with external systems and expect/rely on and generic instrumentation provided by sda-commons.

If additional internal behaviour should to be traced, an OpenTelemetry instance can be acquired using `GlobalOpenTelemetry.get()`.
A very basic skeleton for a creating more traces:
```java

public class Component {
  // ...
  public void doSomething() {
    // ...
    var tracer = GlobalTelemetry.get().getTracer("sda-commons.component");
    Span span = tracer.spanBuilder("doSomething").startSpan();
    try (Scope ignored = span.makeCurrent()) {
      // The actual work
    } finally {
      span.end();
    }
  }
}
```

Some examples for manual tracing can be found in [OpenTelemetry manual tracing example](../sda-commons-server-opentelemetry-example).