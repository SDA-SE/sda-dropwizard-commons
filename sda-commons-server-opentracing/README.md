# SDA Commons Server Open Tracing

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-opentracing/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-opentracing)

This module provides the [`OpenTracingBundle`](./src/main/java/org/sdase/commons/server/opentracing/OpenTracingBundle.java) used to instrument using [OpenTracing](https://opentracing.io/).

First, the module injects an external trace received via HTTP headers and continues new spans inside the existing trace.
In addition, it instruments JAX-RS with different spans, like HTTP requests after resource matching and entity serialisation.

This module doesn't work standalone, as it requires a collector to send traces to a central instance to view them.
Therefore it is required to also include the [`JaegerBundle`](../sda-commons-server-jaeger/README.md) inside the application.


## Initialization

To activate the OpenTracing instrumentation, the `OpenTracingBundle` needs to be added to the application, no further configuration required.
Make sure that the `JaegerBundle` is initialized before the `OpenTracingBundle`.

```
   @Override
   public void initialize(Bootstrap<Configuration> bootstrap) {
      bootstrap.addBundle(JaegerBundle.builder().build());
      ...
      // Other OpenTracing bundles
      bootstrap.addBundle(OpenTracingBundle.builder().build());
      ...
   }
```

It is also possible to use the builder `withTracer(Tracer tracer)` to inject a different tracer, instead of the `GlobalTracer`.
This can be useful for testing, in this case a [`MockTracer`](https://github.com/opentracing/opentracing-java/blob/master/opentracing-mock/README.md) can be injected.


## Instrumentation

In most cases the existing instrumentation of sda-commons should be sufficient.
However to observe internal behavior of a service, manual instrumentation might be required.
Manual instrumentation can be done using the [opentracing-java](https://github.com/opentracing/opentracing-java) package.
An example of manual instrumentation is available in [`sda-commons-server-opentracing-example`](../sda-commons-server-opentracing-example/README.md).
 