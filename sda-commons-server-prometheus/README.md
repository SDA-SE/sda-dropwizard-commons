# SDA Commons Server Prometheus

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-prometheus/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-prometheus)

The module `sda-commons-server-prometheus` provides 

- an admin endpoint to serve metrics in a format that Prometheus can read. The endpoint is available at the applications 
  admin port at `/metrics/prometheus`
- an admin endpoint te server Health Check results as prometheus metrics. The endpoint is available at the applications 
   admin port at `/healthcheck/prometheus`

## Provided metrics

Default metrics that are provided at `/metrics/prometheus`:

| Metric name                       | Labels                | Description                                                  | Source                                    |
|-----------------------------------|-----------------------|--------------------------------------------------------------|-------------------------------------------|
| **http_request_duration_seconds** |                       | Tracks the time needed to handle a request                   | `RequestDurationFilter`                   | 
|                                   | _implementing_method_ | The name of the method that handled the request.             | Request Context                           |
|                                   | _http_method_         | The Http method the client used for the request.             | Request Context                           |
|                                   | _resource_path_       | The mapped path of the request with path param placeholders. | Request Context                           |
|                                   | _status_code_         | The Http status code sent with the response.                 | Response Context                          |
|                                   | _consumer_name_       | Name of the consumer that started the request.               | Request Context Property `Consumer-Name`* |
| **kafka_consumer_records_lag**    |                       | See https://kafka.apache.org/documentation/#consumer_fetch_monitoring | Bridged from Kafka               | 
|                                   | _consumer_name_       | Name of the consumer that processed the message              | Bridged from Kafka                        |
|                                   | _topic_name_          | Name of the topic where messages where consumed from         | Bridged from Kafka                        |
| **kafka_consumer_topic_message_duration**                 | Tracks the time needed to handle consumed Kafka message      | `MessageListener`                         |
|                                   | _consumer_name_       | Name of the consumer that processed the message              | Bridged from Kafka                        |
|                                   | _topic_name_          | Name of the topic where messages where consumed from         | Bridged from Kafka                        |
| **kafka_producer_topic_message_total**                    | Tracks the number of messaged published to a Kafka topic     | `KafkaMessageProducer`                    |
|                                   | _consumer_name_       | Name of the consumer that processed the message              | Bridged from Kafka                        |
|                                   | _topic_name_          | Name of the topic where messages where consumed from         | Bridged from Kafka                        |
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
 
