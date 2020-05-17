# SDA Commons Server Circuit Breaker

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-circuitbreaker/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-circuitbreaker)

This module provides the [`CircuitBreakerBundle`](src/main/java/org/sdase/commons/server/circuitbreaker/CircuitBreakerBundle.java), 
a Dropwizard bundle that is used to inject circuit breakers into service calls.

A [circuit breaker is a pattern](https://martinfowler.com/bliki/CircuitBreaker.html) to make synchronous calls in a distributed system more resilient.
This is especially relevant, if a called service hangs without a response, as such failures would 
cascade up into other services and influence the overall stability of the system.
This module doesn't provide an own implementation, but uses the [circuit breaker](https://resilience4j.readme.io/docs/circuitbreaker)
from [resilience4j](https://github.com/resilience4j/resilience4j).
The bundle also provides prometheus metrics that can be exported in combination with [`sda-commons-server-prometheus`](../sda-commons-server-prometheus/README.md).

## Usage

To create a circuit breaker, register the circuit breaker bundle in the application:

```java
private final CircuitBreakerBundle<AppConfiguration> circuitBreakerBundle = CircuitBreakerBundle
      .builder()
      .withDefaultConfig()
      .build();

@Override
public void initialize(Bootstrap<AppConfiguration> bootstrap) {
   bootstrap.addBundle(circuitBreakerBundle);
   
   ...
}
```

As the configuration depends on the environment and the load on the service, a different configuration 
might be required. A custom configuration can be specified globally at the bundle:

```java
private final CircuitBreakerBundle<AppConfiguration> circuitBreakerBundle = CircuitBreakerBundle
      .builder()
      .<AppConfiguration>withCustomConfig(new CircuitBreakerConfiguration().setFailureRateThreshold(50.0f))
      .build();
```

It's also possible to load the configuration from the Dropwizard configuration:

```java
private final CircuitBreakerBundle<AppConfiguration> circuitBreakerBundle = CircuitBreakerBundle
      .builder()
      .withConfigProvider(AppConfiguration::getCircuitBreaker)
      .build();
```

## Creating a Circuit Breaker

New circuit breakers can be created from the bundle using a builder pattern:

```java
CircuitBreaker circuitBreaker = circuitBreakerBundle
      .createCircuitBreaker("nameInMetrics")
      .withDefaultConfig()
      .build();
```

In case the global bundle configuration isn't sufficient for the specific instance, a custom 
configuration can be provided (or loaded from the Dropwizard config using `withConfigProvider`):

```java
CircuitBreaker circuitBreaker = circuitBreakerBundle
      .createCircuitBreaker("nameInMetrics")
      .withCustomConfig(new CircuitBreakerConfiguration()
            .setFailureRateThreshold(75.0f))
      .build();
```

Method calls can be wrapped using the circuit breaker:

```java
circuitBreaker.executeSupplier(() -> target.doAction());
```

See the [resilience4j documentation](https://resilience4j.github.io/resilience4j/#_circuitbreaker) 
for a full usage guide.

## Wrapping with Proxies

In case you would like to wrap all calls to an object with a circuit breaker, the module provides a 
simple way to achieve this. For example, a Jersey client can be wrapped with a proxy:

```java
serviceClient = circuitBreakerBundle
      .createCircuitBreaker("ServiceClient")
      .withDefaultConfig())
      .wrap(jerseyClientBundle
          .getClientFactory()
          .platformClient()
          .enableAuthenticationPassThrough()
          .enableConsumerToken()
          .api(ServiceClient.class)
          .atTarget(configuration.getUrl()));

serviceClient.doAction();
```
