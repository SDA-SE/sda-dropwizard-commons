# SDA Commons Server Prometheus

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-prometheus/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-prometheus)

The module `sda-commons-server-prometheus` provides an admin endpoint to serve metrics and health check results in a format that Prometheus can read. 
The endpoint is available at the applications admin port at `/metrics/prometheus`.

## Provided metrics

Default metrics that are provided at `/metrics/prometheus`:

| Metric name                       | Labels                | Description                                                  | Source                                    |
|-----------------------------------|-----------------------|--------------------------------------------------------------|-------------------------------------------|
| **`http_request_duration_seconds`** |                       | Tracks the time needed to handle a request                   | `RequestDurationFilter`                   | 
|                                   | _`implementing_method`_ | The name of the method that handled the request.             | Request Context                           |
|                                   | _`http_method`_         | The HTTP method the client used for the request.             | Request Context                           |
|                                   | _`resource_path`_       | The mapped path of the request with path param placeholders. | Request Context                           |
|                                   | _`status_code`_         | The HTTP status code sent with the response.                 | Response Context                          |
|                                   | _`consumer_name`_       | Name of the consumer that started the request.               | Request Context Property `Consumer-Name`* |
| **`kafka_consumer_records_lag`**    |                       | See [Kafka Documentation](https://kafka.apache.org/documentation/#consumer_fetch_monitoring) | Bridged from Kafka               | 
|                                   | _`consumer_name`_       | Name of the consumer that processed the message              | Bridged from Kafka                        |
|                                   | _`topic_name`_          | Name of the topic where messages where consumed from         | Bridged from Kafka                        |
| **`kafka_consumer_topic_message_duration`**                 | Tracks the time needed to handle consumed Kafka message      | `MessageListener`                         |
|                                   | _`consumer_name`_       | Name of the consumer that processed the message              | Bridged from Kafka                        |
|                                   | _`topic_name`_          | Name of the topic where messages where consumed from         | Bridged from Kafka                        |
| **`kafka_producer_topic_message_total`**                    | Tracks the number of messaged published to a Kafka topic     | `KafkaMessageProducer`                    |
|                                   | _`consumer_name`_       | Name of the consumer that processed the message              | Bridged from Kafka                        |
|                                   | _`topic_name`_          | Name of the topic where messages where consumed from         | Bridged from Kafka                        |
| **`jvm_`***                       |                         | Multiple metrics about the JVM                               | Bridged from Dropwizard                   |
| **`io_dropwizard_jetty_`**        |                         | Multiple metrics from the embedded Jetty server              | Bridged from Dropwizard                   |
| **`io_dropwizard_db_`**           |                         | Multiple metrics from the database if a database is used     | Bridged from Dropwizard                   |
| **`healthcheck_status`**          | _`name`_                | Metrics that represent the state of the health checks        | `HealthCheckMetricsCollector`             |
| **`healthcheck_status_overall`**  |                         | Aggregated metric over all healthcheck_status checks         | `HealthCheckMetricsCollector`             |

*) A filter that extracts the consumer from the HTTP headers should add `Consumer-Name` to the request properties. That
   filter is not part of the `PrometheusBundle`.

## Health Checks

All health checks are provided as a Gauge metric `healthcheck_status` and are included in the metrics endpoint.
They are also available at the applications admin port at `/healthcheck/prometheus`.
This endpoint is only available for backward compatibility and will be removed in the future.
The name of the Health Check is used as label `name`.
The metric value is `1.0` for healthy and `0.0` for unhealthy. Example:

```
healthcheck_status{name="hibernate",} 1.0
healthcheck_status{name="disk_space",} 0.0
```

Additionaly to the metric `healthcheck_status` there is the metric `healthcheck_status_overall`, which aggregates each `healthcheck_status`
to a combined metric, because if any of the singular `healthcheck_status` fails, it results in the service being not available (kubernetes readiness probe).
This metric is exposed to avoid the calculation in prometheus, because it could run fairly frequently for alerting or over long time frames.
The metric value is `1.0` as long as all singular `healthcheck_status` return the value `1.0`. As soon as one of all the `healthcheck_status` returns the value `0.0`,
the metric value for `healthcheck_status_overall` will be `0.0`.

```
healthcheck_status_overall 1.0
```

Currently health checks are evaluated when their status is requested. Slow health checks should be annotated with 
[`com.codahale.metrics.health.annotation.Async`](https://github.com/dropwizard/metrics/blob/v4.0.2/metrics-healthchecks/src/main/java/com/codahale/metrics/health/annotation/Async.java)
to avoid blocking collection of the results.

## Usage

The [`PrometheusBundle`](./src/main/java/org/sdase/commons/server/prometheus/PrometheusBundle.java) has to be added in
the application:

```java
import PrometheusBundle;
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
 
