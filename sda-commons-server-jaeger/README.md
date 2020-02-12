# SDA Commons Server Jaeger

This module provides the [`JaegerBundle`](./src/main/java/org/sdase/commons/server/jaeger/JaegerBundle.java) used to collect [OpenTracing](https://opentracing.io/) traces to [Jaeger](https://www.jaegertracing.io/).
When traces are generated, the `JaegerBundle` forwards the traces to the Jaeger agent.
The Jaeger collector is registered in the `GlobalTracer`.

The bundle has no function when used standalone, it always has to be used together with other bundles that are using OpenTracing instrumentation.

For local testing, a simple Jaeger instance (the all-in-one image) is available at [Docker Hub](https://hub.docker.com/r/jaegertracing/all-in-one).


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

While the bundle comes with defaults, often additional configuration is required.
The full list of configuration variables is listed [here](https://github.com/jaegertracing/jaeger-client-java/blob/master/jaeger-core/README.md#configuration-via-environment).


### Jaeger Agent

Spans are forwarded from the application to the Jaeger agent.
In a Kubernetes environment the Jaeger agent can either be deployed as a sidecar or on each node.
By default the `JaegerBundle` forwards traces to a agent deployed as a sidecar on `localhost`.
However, in production or local testing scenarios it might be required to configure a different host for the agent.
Therefore the bundle can be configured using environment variables.
Set the environment variable `JAEGER_AGENT_HOST` to your desired hostname.
In a Kubernetes environment where the agent is deployed on each node, use the following configuration:

```yaml
- name: JAEGER_AGENT_HOST
  valueFrom:
    fieldRef:
      fieldPath: status.hostIP
```


### Service Name

The service name is displayed in the traces.
Therefore it's important that every instance of a service has a unique name.
By default, the service name is filled with the name of the Dropwizard application.
To override it with a custom name for each deployment, pass the `JAEGER_SERVICE_NAME` environment variable.


### Sampling

By default, sampling is configured to sample every trace.
This is fine for testing or development, but might be to much data for production.
Therefore specify `JAEGER_SAMPLER_TYPE` and `JAEGER_SAMPLER_PARAM` in your deployments, see the [sampler types](https://www.jaegertracing.io/docs/1.16/sampling/#client-sampling-configuration) for more information.
