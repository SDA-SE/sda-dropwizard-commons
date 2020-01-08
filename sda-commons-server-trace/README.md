# SDA Commons Server Trace
  
The module `sda-commons-server-trace` adds support to track or create a trace token. The trace token is used to
correlate a set of service invocations that belongs to the same logically cohesive call of a higher level service offered by 
the SDA platform, e.g. interaction service. 

Every service must forward the received trace token within calls to other services that are part of the SDA platform. In
HTTP REST calls, it is done within the HTTP Header  as `Trace-Token`. 

If no token is provided within the request, it will be generated automatically and put into the 
request context. This should be only the case, if a service call firstly enters the SDA platform.

The calls can be correlated in logs and metrics using this trace token that is also added to the 
[`MDC`](https://www.slf4j.org/manual.html#mdc).

When using new threads for clients to invoke another service, the trace token is not transferred out-of-the-box. 
The same holds for mentioning the trace token in log entries of new threads.


## Usage

The trace token is loaded within a filter that is created and registered by the 
[`TraceTokenBundle`](src/main/java/org/sdase/commons/server/trace/TraceTokenBundle.java) which must be added
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
