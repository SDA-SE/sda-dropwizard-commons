# SDA Commons Server Prometheus

The module `sda-commons-server-prometheus` provides 

- an admin endpoint to serve metrics in a format that Prometheus can read. The endpoint is available at the applications 
  admin port at `/metrics/prometheus`
- an admin endpoint te server Health Check results as prometheus metrics. The endpoint is available at the applications 
   admin port at `/healthcheck/prometheus`

## Provided metrics

Default metrics that are provided a `/metrics/prometheus`:

| Metric name                       | Labels                | Description                                                  | Source                                    |
|-----------------------------------|-----------------------|--------------------------------------------------------------|-------------------------------------------|
| **http_request_duration_seconds** |                       | Tracks the time needed to handle a request                   | `RequestDurationFilter`                   | 
|                                   | _implementing_method_ | The name of the method that handled the request.             | Request Context                           |
|                                   | _http_method_         | The Http method the client used for the request.             | Request Context                           |
|                                   | _resource_path_       | The mapped path of the request with path param placeholders. | Request Context                           |
|                                   | _status_code_         | The Http status code sent with the response.                 | Response Context                          |
|                                   | _consumer_name_       | Name of the consumer that started the request.               | Request Context Property `Consumer-Name`* |
| **jvm_***                         |                       | Multiple metrics about the JVM                               | Bridged from Dropwizard                   |
| **io_dropwizard_jetty_***         |                       | Multiple metrics from the embedded Jetty server              | Bridged from Dropwizard                   |
| **io_dropwizard_db_***            |                       | Multiple metrics from the database if a database is used     | Bridged from Dropwizard                   |

*) A filter that extracts the consumer from the Http headers should add `Consumer-Name` to the request properties. That
   filter is not part of the `PrometheusBundle`.

## Health Checks

All health checks are provided at the applications admin port at `/healthcheck/prometheus` as Gauge metric 
`healthcheck_status`. The name of the Health Check is used as label `name`. The metric value is `1.0` for healthy and
`0.0` for unhealthy. Example:

```
healthcheck_status{name="hibernate",} 1.0
healthcheck_status{name="disk_space",} 0.0
```

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
 