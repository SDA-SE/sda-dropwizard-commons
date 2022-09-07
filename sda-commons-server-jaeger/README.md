# SDA Commons Server Jaeger `Deprecated`

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-jaeger/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-jaeger)

> **_NOTE:_** This module is deprecated in favour of [sda-commons-server-opentelemetry](../sda-commons-server-opentelemetry).

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
  value: "jaeger-agent.jaeger"
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

### Disabling Jaeger Tracing

If you are running your service in an environment where Jaeger is unavailable, you might want to disable the Jaeger tracing.
While it's not a problem if sampling data can't be reported to the Jaeger backend, you might get the following warning in your logs:

```
reporters.RemoteReporter - FlushCommand execution failed! Repeated errors of this command will not be logged.
io.jaegertracing.internal.exceptions.SenderException: Failed to flush spans.
	at io.jaegertracing.thrift.internal.senders.ThriftSender.flush(ThriftSender.java:115)
	at io.jaegertracing.internal.reporters.RemoteReporter$FlushCommand.execute(RemoteReporter.java:160)
	at io.jaegertracing.internal.reporters.RemoteReporter$QueueProcessor.run(RemoteReporter.java:182)
	at java.base/java.lang.Thread.run(Unknown Source)
Caused by: io.jaegertracing.internal.exceptions.SenderException: Could not send 5 spans
	at io.jaegertracing.thrift.internal.senders.UdpSender.send(UdpSender.java:85)
	at io.jaegertracing.thrift.internal.senders.ThriftSender.flush(ThriftSender.java:113)
	... 3 common frames omitted
Caused by: org.apache.thrift.transport.TTransportException: Cannot flush closed transport
	at io.jaegertracing.thrift.internal.reporters.protocols.ThriftUdpTransport.flush(ThriftUdpTransport.java:148)
	at org.apache.thrift.TServiceClient.sendBase(TServiceClient.java:73)
	at org.apache.thrift.TServiceClient.sendBaseOneway(TServiceClient.java:66)
	at io.jaegertracing.agent.thrift.Agent$Client.send_emitBatch(Agent.java:70)
	at io.jaegertracing.agent.thrift.Agent$Client.emitBatch(Agent.java:63)
	at io.jaegertracing.thrift.internal.senders.UdpSender.send(UdpSender.java:83)
	... 4 common frames omitted
Caused by: java.net.PortUnreachableException: ICMP Port Unreachable
	at java.base/java.net.PlainDatagramSocketImpl.send(Native Method)
	at java.base/java.net.DatagramSocket.send(Unknown Source)
	at io.jaegertracing.thrift.internal.reporters.protocols.ThriftUdpTransport.flush(ThriftUdpTransport.java:146)
	... 9 common frames omitted
```

To disable tracing you have to configure it to sample no spans.
This can be done by setting the combination of `JAEGER_SAMPLER_TYPE=const` and `JAEGER_SAMPLER_PARAM=0`.
