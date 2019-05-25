# SDA Commons Server Jaeger

This module provides the [`JaegerBundle`](./src/main/java/org/sdase/commons/server/jaeger/JaegerBundle.java) used to collect [OpenTracing](https://opentracing.io/) traces to [Jaeger](https://www.jaegertracing.io/).
When traces are generated, the `JaegerBundle` forwards the traces to the Jaeger agent.
The Jaeger collector is registered in the `GlobalTracer`.

The bundle has no function when used standalone, it always has to be used together with other bundles that are using OpenTracing instrumentation.


## Initialization

The bundle can be added during application startup, no further configuration required.
Make sure that the `JaegerBundle` is initialized before other bundles that are using OpenTracing instrumentation.

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

## Configuration

By default the `JaegerBundle` forwards traces to the Jaeger agent on `localhost`.
In a production scenario such a Jaeger agent is deployed as a sidecar in every pod and therefore accessible at `localhost`. 

However, in local testing scenarios it might be required to configure a different host for the agent.
Therefore the bundle can be configured using environment variables.
The full list of configuration variables is listed [here](https://github.com/jaegertracing/jaeger-client-java/blob/master/jaeger-core/README.md#configuration-via-environment).

For local testing, a simple Jaeger instance (all-in-one image) is available in [local-infra](https://github.com/SDA-SE/local-infra).
Set the environment variable `JAEGER_AGENT_HOST` to your desired hostname.
