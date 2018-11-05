# SDA Commons Server Prometheus

The module `sda-commons-server-prometheus` provides an admin endpoint to serve metrics in a format that Prometheus can 
read. The endpoint is available at the applications admin port at `/metrics/prometheus`

The module registers a response duration metric (`http_request_duration_seconds`) with some additional tags. The metric
will be published with a label `consumer_name` if somebody (later a filter that extracts the consumer from Http headers)
adds a consumer name as `Consumer-Name` to the request attributes.
 
Additionally the module bridges the default metrics provided by Dropwizard.



## Usage

The [`PrometheusBundle`](./src/main/java/com/sdase/commons/server/prometheus/PrometheusBundle.java) has to be added in
the application:

```java
import com.sdase.commons.server.prometheus.PrometheusBundle;
import io.dropwizard.Application;

public class MyApplication extends Application<MyConfiguration> {
   
    public static void main(final String[] args) {
        new MyApplication().run(args);
    }

   @Override
   public void initialize(Bootstrap<MyConfiguration> bootstrap) {
      // ...
      bootstrap.addBundle(PrometheusBundle.builder().build());
      // ...
   }

   @Override
   public void run(MyConfiguration configuration, Environment environment) {
      // ...
   }
}
```
 