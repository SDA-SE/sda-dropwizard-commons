# SDA Commons Shared OpenTelemetry tracing Example

This module is an example for a service with manual instrumentation using [OpenTelemetry](https://opentelemetry.io/).

The usage of the modules [sda-commons-server-opentracing](../sda-commons-server-opentracing/README.md) and [sda-commons-shared-otel-agent](../sda-commons-shared-otel-agent/README.md) for custom manual instrumentation.
See the [`OtelTracingApplication`](./src/main/java/org/sdase/shared/otel/tracing/example/OtelTracingApplication.java) for the examples.

## How to run the example

Start the example app and pass `server config.yml` as command line arguments and 
- `otel.traces.exporter=jaeger`
- `otel.exporter.jaeger.endpoint=http://jaeger-host:14250`.
You also have to start Jaeger, for example using the [Jaeger all-in-one image](https://hub.docker.com/r/jaegertracing/all-in-one).
Afterwards you can perform the following requests:

- [http://localhost:8080/](http://localhost:8080/)
- [http://localhost:8080/recursive](http://localhost:8080/recursive)
- [http://localhost:8080/error](http://localhost:8080/error)
- [http://localhost:8080/search](http://localhost:8080/search)
- [http://localhost:8080/instrumented](http://localhost:8080/instrumented)
- [http://localhost:8080/param/value](http://localhost:8080/param/value)