# SDA Commons Server Dropwizard

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-dropwizard/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-dropwizard)

`sda-commons-server-dropwizard` is an aggregator module that provides `io.dropwizard:dropwizard-core` with convergent
dependencies and some common Dropwizard bundles for easier configuration that are not dependent on other technology
than Dropwizard.

This module fixes some version mixes of transitive dependencies in Dropwizard. Dependency convergence should be checked 
with `gradlew dependencies` on upgrades.

## Provided Bundles

The Dropwizard module provides default Bundles that are useful for most Dropwizard applications.

### ConfigurationSubstitutionBundle

The [`ConfigurationSubstitutionBundle`](./src/main/java/org/sdase/commons/server/dropwizard/bundles/ConfigurationSubstitutionBundle.java)
allows to use placeholders for environment variables or system properties in the `config.yaml` of
the application to dynamically configure the application at startup. Default values can be added
after the variable name separated by `:-`

The `ConfigurationSubstitutionBundle` supports the modifier `toJsonString` to convert String values
in the `config.yaml` into valid Json String values by escaping the value and wrapping it in quotes.
The default value of a value modified by `toJsonString` must be valid Json itself, e.g. `null` or
`"my\nmultiline\nstring"`.

```yaml
database:
  driverClass: org.postgresql.Driver
  user: ${POSTGRES_USER:-dev}
  password: ${POSTGRES_PASSWORD | toJsonString:-"s3cr3t"}
  url: ${POSTGRES_URL:-localhost:12345}
```

### ConfigurationValueSupplierBundle

The [`ConfigurationValueSupplierBundle`](./src/main/java/org/sdase/commons/server/dropwizard/bundles/ConfigurationValueSupplierBundle.java)
provides a `Supplier` for a configuration value. It may be used if the type of the configuration itself should not be 
known by the class that is configured. This may be the case if another bundle or a service should be configured either
by a configuration property or another service.

A configuration value may be supplied like this:

```java
public class MyApplication extends Application<MyConfiguration> {
   
    public static void main(final String[] args) {
        new MyApplication().run(args);
    }

   @Override
   public void initialize(Bootstrap<MyConfiguration> bootstrap) {
      // ...
      ConfigurationValueSupplierBundle<MyConfiguration, String> configStringBundle =
            ConfigurationValueSupplierBundle.builder().withAccessor(MyConfiguration::getConfigString).build();
      bootstrap.addBundle(configStringBundle);
      Supplier<Optional<String>> configStringSupplier = configStringBundle.supplier();
      // configStringSupplier may be added to other bundles and services, it's get() method can be access after run()
      // ...
   }

   @Override
   public void run(MyConfiguration configuration, Environment environment) {
      // ...
   }
}
```

### DefaultLoggingConfigurationBundle
The [`DefaultLoggingConfigurationBundle`](./src/main/java/org/sdase/commons/server/dropwizard/bundles/DefaultLoggingConfigurationBundle.java), 
is used to configure the console logger with the settings desired by the SDA.

The bundle sets the log threshold for the console appender to `INFO` and uses the following log format:

```
[%d] [%-5level] [%X{Trace-Token}] %logger{36} - %msg%n
```

Make sure to add the bundle **after the `ConfigurationSubstitutionBundle`** if it's present.
Logging related configuration is not required by this bundle. 

```
public void initialize(Bootstrap<Configuration> bootstrap) {
    bootstrap.addBundle(DefaultLoggingConfigurationBundle.builder().build());
}
```

### MetadataContextBundle
The [`MetadataContextBundle`](./src/main/java/org/sdase/commons/server/dropwizard/bundles/MetadataContextBundle.java)
enables the metadata context handling for an application.

If you want to make use of the data in the metadata context, you should read the [dedicated documentation](./docs/metadata-context.md).
If your service is required to support the metadata context but is not interested in the data,
continue here:

Services that use the bundle
- can access the current [`MetadataContext`](./src/main/java/org/sdase/commons/server/dropwizard/metadata/MetadataContext.java)
  in their implementation
- will automatically load the context from incoming HTTP requests into the thread handling the
  request
- will automatically load the context from consumed Kafka messages into the thread handling the
  message and the error when handling the message fails when the consumer is configured with one of
  the provided `MessageListenerStrategy` implementations
- will automatically propagate the context to other services via HTTP when using a platform client
  from the `JerseyClientBundle`
- will automatically propagate the context in produced Kafka messages when the producer is created
  or registered by the `KafkaBundle`
- are configurable by the property or environment variable `METADATA_FIELDS` to be aware of the
  metadata used in a specific environment

Services that interrupt a business process should persist the context from
`MetadataContext.detachedCurrent()` and restore it with `MetadataContext.createContext(…)` when the
process continues.
Interrupting a business process means that processing is stopped and continued later in a new thread
or even another instance of the service.
Most likely, this will happen when a business entity is stored based on a request and loaded later
for further processing by a scheduler or due to a new user interaction.
In this case, the `DetachedMetadataContext` must be persisted along with the entity and recreated
when the entity is loaded.
The `DetachedMetadataContext` can be defined as field in any MongoDB entity.

Services that handle requests or messages in parallel must transfer the metadata context to their
`Runnable` or `Callable` with `MetadataContext.transferMetadataContext(…)`.
In most cases, developers should prefer`ContainerRequestContextHolder.transferRequestContext(…)`,
which also transfers the metadata context.

Services that use the `MetadataContextBundle` and take care of interrupted processes and parallel
execution, may add a link like this in their deployment documentation:

```md
This service keeps track of the [metadata context](https://github.com/SDA-SE/sda-dropwizard-commons/blob/master/sda-commons-server-dropwizard/README.md#metadatacontextbundle).
```

### JSON Logging
To enable [JSON logging](https://www.dropwizard.io/en/latest/manual/core.html#logging), set the environment variable `ENABLE_JSON_LOGGING` to `"true"`.
We recommend JSON logging in production as they are better parsable by tools.
However they are hard to read for human beings, so better deactivate them when working with a service locally.

### Healthcheck Request Logging
By default, Dropwizard logs all incoming requests to the console, this includes healthchecks like `/ping` and `/healthcheck/internal`.
As these are very frequent and can quickly pollute the logs, they can be disabled by setting the environment variable `DISABLE_HEALTHCHECK_LOGS` to `"true"`.
This will be overwritten by any manual configuration to the FilterFactories.
With `DISABLE_HEALTHCHECK_LOGS` active request logs for all paths related to monitoring are disabled:
- `/ping`
- `/healthcheck`
- `/healthcheck/internal`
- `/metrics`
- `/metrics/prometheus`
