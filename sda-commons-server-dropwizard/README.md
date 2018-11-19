# SDA Commons Server Dropwizard

`sda-commons-server-dropwizard` is an aggregator module that provides `io.dropwizard:dropwizard-core` with convergent
dependencies and some common Dropwizard bundles for easier configuration that are not dependent on other technology
than Dropwizard.

This module fixes some version mixes of transitive dependencies in dropwizard. Dependency convergence should be checked 
with `gradlew dependencies` on upgrades.

## Provided Bundles

The Dropwizard module provides default Bundles that are useful for most Dropwizard applications.

### ConfigurationSubstitutionBundle

The [`ConfigurationSubstitutionBundle`](./src/main/java/org/sdase/commons/server/dropwizard/bundles/ConfigurationSubstitutionBundle.java)
allows to use placeholders for environment variables in the config.yaml of the application to dynamically configure the
application at startup. Default values can be added after the environment variable name separated by `:-`

```yaml
database:
  driverClass: org.postgresql.Driver
  user: ${POSTGRES_USER:-dev}
  password: ${POSTGRES_PASSWORD:-s3cr3t}
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
