# SDA Dropwizard Commons

SDA Dropwizard Commons is a set of libraries to bootstrap services easily that follow the patterns and specifications promoted by
the SDA SE.


>
> ##### ⚠️ ATTENTION: Please use SDA Dropwizard Commons version 4 and newer:
>
> New features can only be contributed in version 4.x.x.
>
> Version 2.x.x only receives critical security updates until October 2023.
> We will only provide security updates for the libraries that are still compatible with Java 8.
>
> Please make sure to upgrade to version 4 as soon as possible.


SDA Dropwizard Commons is separated in different modules that can be combined as needed. Most of the modules require the
technologies that are recommended for services in the SDA Platform. These technologies include

- [Dropwizard](https://www.dropwizard.io)
- [Jackson](https://github.com/FasterXML/jackson)
- [JAX-RS](https://jcp.org/en/jsr/detail?id=339)
- [Jersey](https://jersey.github.io/)
- [Swagger](https://swagger.io/)
- [Hibernate](http://hibernate.org/)
- [Kafka](https://kafka.apache.org/)
- [MongoDB](https://www.mongodb.com)
- [Open Policy Agent](https://www.openpolicyagent.org/)
- [OpenTelemetry](https://opentelemetry.io/)

## Modules in SDA Dropwizard Commons


### Server

All modules prefixed with `sda-commons-server-` provide technology and configuration support used in backend services
to provide REST Endpoints.


#### Main Server Modules

The main server modules help to bootstrap and test a Dropwizard application with convergent dependencies.


##### Starter

The module [`sda-commons-starter`](./starter.md) provides all basics required to build
a service for the SDA Platform with Dropwizard.

The module [`sda-commons-starter-example`](./starter-example.md) gives a small example
on starting an application using defaults for the SDA Platform.

##### Testing

The module [`sda-commons-server-testing`](./server-testing.md) is the base module to add unit and
integration tests for applications in the SDA SE infrastructure.

Some modules have a more specialized testing module, e.g. the
[`sda-commons-server-hibernate`](./server-hibernate.md) module has a
[`sda-commons-server-hibernate-testing`](./server-hibernate-testing.md) module, providing further
support.


#### Additional Server Modules

The additional server modules add helpful technologies to the Dropwizard application.


##### Auth

The module [`sda-commons-server-auth`](./server-auth.md) provides support to add authentication
using JSON Web Tokens with different sources for the public keys of the signing authorities.


##### Circuit Breaker

The module [`sda-commons-server-circuitbreaker`](./server-circuit-breaker.md) provides support to
inject circuit breakers into synchronous calls to other services.


##### Consumer Token

The module [`sda-commons-server-consumer`](./server-consumer.md) adds support to track or require a
consumer token identifying the calling application.


##### Cross-Origin Resource Sharing

The module [`sda-commons-server-cors`](./server-cors.md) adds support for CORS. This allows
Cross-origin resource sharing for the service.

##### Forms

The module [`sda-commons-shared-forms`](./shared-forms.md) adds all required dependencies to support
`multipart/*` in Dropwizard applications.

##### Dropwizard

The module [`sda-commons-server-dropwizard`](./server-dropwizard.md) provides
`io.dropwizard:dropwizard-core` with convergent dependencies. All other SDA Dropwizard Commons Server modules use this dependency
and are aligned to the versions provided by `sda-commons-server-dropwizard`. It also provides some common bundles that
require no additional dependencies.


##### Health Check
The module [`sda-commons-server-healthcheck`](./server-healthcheck.md) introduces the possibility
to distinguish internal and external health checks.

The module [`sda-commons-server-healthcheck-example`](./server-healthcheck-example.md)
presents a simple application that shows the usage of the bundle and implementation of new health checks.


##### Hibernate

The module [`sda-commons-server-hibernate`](./server-hibernate.md) provides access to relational
databases with hibernate.

The module [`sda-commons-server-hibernate-exmaple`](./server-hibernate-example.md) shows how
to use the bundle within an application.


##### Jackson

The module [`sda-commons-server-jackson`](./server-jackson.md) is used for several purposes
* configure the `ObjectMapper` with the recommended default settings of SDA SE services.
* provides support for linking resources with HAL
* adds the ability to filter fields on client request
* registers exception mapper to support the common error structure as defined within the rest guide


##### Kafka

The module [`sda-commons-server-kafka`](./server-kafka.md) provides means to send and consume
messages from a Kafka topic.

The module [`sda-commons-server-kafka-example`](./server-kafka-example.md) includes
applications, one with consumer and one with producer examples.

##### Key Management

The module [`sda-commons-server-key-mgmt`](./server-key-mgmt.md) provides means to provide
and map enumerations (keys) between the API data model and a possible implementation.

##### MongoDB

The module [`sda-commons-server-spring-data-mongo`](./server-spring-data-mongo.md) is used to work
with MongoDB using [Spring Data Mongo](https://spring.io/projects/spring-data-mongodb).

The module [`sda-commons-server-mongo-testing`](./server-mongo-testing.md)
provides a MongoDB instance for integration testing.

The [`example`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/master/sda-commons-server-spring-data-mongo/src/test/java/org/sdase/commons/server/spring/data/mongo/example)
package shows how to use the bundle within an application.

##### Open Telemetry

The module [`sda-commons-server-opentelemetry`](./server-opentelemetry.md) provides [OpenTelemetry](https://opentelemetry.io/) instrumentation for JAX-RS.
Other bundles like `sda-commons-client-jersey`, `sda-commons-server-spring-data-mongo` or `sda-commons-server-s3` come with built-in instrumentation.

Besides instrumentation, it's also required to specify a collector, like [Jaeger](https://www.jaegertracing.io/).

The module [`sda-commons-server-opentelemetry-example`](./server-opentelemetry-example.md) shows how to use OpenTelemetry and Jaeger within an application and has examples for manual instrumentation.


##### Prometheus

The module [`sda-commons-server-prometheus`](./server-prometheus.md) provides an admin endpoint to
serve metrics in a format that Prometheus can read.

The module [`sda-commons-server-prometheus-example`](./server-prometheus-example.md)
presents a simple application that shows the three main types of metrics to use in a service.


##### S3 Object Storage

The module [`sda-commons-server-s3`](./server-s3.md) provides a client for an
AWS S3-compatible object storage.

The module [`sda-commons-server-s3-testing`](./server-s3-testing.md) is used to
provide an [AWS S3-compatible Object Storage](https://docs.aws.amazon.com/AmazonS3/latest/API/Welcome.html) during integrations tests.


##### Security

The module [`sda-commons-server-security`](./server-security.md) helps to configure a secure
Dropwizard application.


##### Shared Certificates

The module [`sda-commons-shared-certificates`](./shared-certificates.md) adds support for trusting custom certificate authorities.

##### Trace Token

The module [`sda-commons-server-trace`](./server-trace.md) adds support to track create a
trace token to correlate  a set of service invocations that belongs to the same logically cohesive call of a higher
level service offered by the SDA Platform, e.g. interaction service. .


##### Weld

The module [`sda-commons-server-weld`](./server-weld.md) is used to bootstrap Dropwizard applications
inside a Weld-SE container and provides CDI support for servlets, listeners and resources.

The module [`sda-commons-server-weld-example`](./server-weld-example.md) gives a small example on
starting an application within a Weld container.


##### YAML

The module [`sda-commons-shared-yaml`](./shared-yaml.md) adds support for YAML-file handling.


### Client

All modules prefixed with `sda-commons-client-` provide support for applications that use an HTTP client to access other
services.


#### Jersey

The module [`sda-commons-client-jersey`](./client-jersey.md) provides support for using Jersey
clients withing the Dropwizard application.

The module [`sda-commons-client-jersey-wiremock-testing`](./client-jersey-wiremock-testing.md)
bundles the [WireMock](https://wiremock.org) dependencies to mock services in integration tests consistently to
sda-commons library versions.

The module [`sda-commons-client-jersey-example`](./client-jersey-example.md)
presents an example application that shows how to invoke services.

## Usage

The compiled releases are publicly available via [maven central](https://search.maven.org/search?q=org.sdase.commons).

Include `sda-commons-bom` and `sda-commons-dependencies` as platform constraints. You will inherit
all versions defined there and won't have to specify versions for them yourself.

More details:
- [sda-commons-bom](./bom.md)
- [sda-commons-dependencies](./dependencies.md)

Note: You need Gradle 5.x for platform dependencies. [More information can be found here](https://gradle.org/whats-new/gradle-5/).

```gradle
    project.ext {
        sdaCommonsVersion = 'x.x.x'
    }

    dependencies {
      // define platform dependencies for simplified dependency management
      compile enforcedPlatform("org.sdase.commons.sda-commons-dependencies:$sdaCommonsVersion")
      compile enforcedPlatform("org.sdase.commons.sda-commons-bom:$sdaCommonsVersion")
      ...

      // Add dependencies to sda-commons-modules (managed by sda-commons-bom)
      compile "org.sdase.commons:sda-commons-client-jersey"
      ...

      // Add other dependencies (managed by 'sda-commons-dependencies')
      compile 'org.glassfish.jersey.core:jersey-client'

      // Add other unmanaged dependencies
      compile 'org.apache.commons:commons-digester3:3.2'
    }
```