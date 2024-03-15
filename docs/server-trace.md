# SDA Commons Server Trace

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-trace/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-trace)

The module `sda-commons-server-trace` adds support to track or create a trace token. The trace token is used to
correlate a set of service invocations that belongs to the same logically cohesive call of a higher level service offered by 
the SDA Platform, e.g. interaction service. 

Every service must forward the received trace token within calls to other services that are part of the SDA Platform. In
HTTP REST calls, it's done within the HTTP Header  as `Trace-Token`. 

If no token is provided within the request, it will be generated automatically and put into the 
request context. This should be only the case, if a service call firstly enters the SDA Platform.

The calls can be correlated in logs and metrics using this trace token that is also added to the 
[`MDC`](https://www.slf4j.org/manual.html#mdc).

When using new threads for clients to invoke another service, the trace token is not transferred out-of-the-box. 
The same holds for mentioning the trace token in log entries of new threads.
See the documentation about [concurrency](./client-jersey.md#concurrency) on how to transfer this context into another thread.


## Usage

The trace token is loaded within a filter that is created and registered by the 
[`TraceTokenBundle`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-server-trace/src/main/java/org/sdase/commons/server/trace/TraceTokenBundle.java) which must be added
to the Dropwizard application:

```java
public class MyApplication extends Application<MyConfiguration> {
   
    public static void main(final String[] args) {
        new MyApplication().run(args);
    }

   @Override
   public void initialize(Bootstrap<MyConfiguration> bootstrap) {
      // ...
      bootstrap.addBundle(TraceTokenBundle.builder().build());
      // ...
   }

   @Override
   public void run(MyConfiguration configuration, Environment environment) {
      // ...
   }
}
```

### Custom initialization

In some cases, Dropwizard can't be configured to start correlation for a specific initialization
point.
For example, a dedicated correlation for each entity may be useful when batch processes act on
multiple entities in the database.
Also, [Dropwizard Tasks](https://www.dropwizard.io/en/release-3.0.x/manual/core.html#tasks) can't be
configured on library level like regular HTTP endpoints although correlation may be desired here as
well.

To cover such individual cases, the library allows to wrap an operation in a `TraceTokenContext`.
This will result in a `Trace-Token` in the log MDC, forwarding the trace token with clients build
with [client-jersey](./client-jersey.md) and forwarding the parent trace token with producers of
[server-kafka](./server-kafka.md).

??? example "Trace Context for Dropwizard Tasks"
    ```java
    --8<-- "./sda-commons-server-trace/src/test/java/org/sdase/commons/server/trace/test/TraceTokenAwareExampleTask.java"
    ```
