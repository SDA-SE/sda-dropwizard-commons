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