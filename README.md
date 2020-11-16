[![Latest Release](https://img.shields.io/github/v/release/sda-se/sda-dropwizard-commons?label=latest)](https://github.com/SDA-SE/sda-dropwizard-commons/releases/latest)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.sdase.commons/sda-commons-starter/badge.svg)](https://search.maven.org/search?q=org.sdase.commons)
[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-bom/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/)
[![Java CI](https://github.com/SDA-SE/sda-dropwizard-commons/workflows/Java%20CI/badge.svg)](https://github.com/SDA-SE/sda-dropwizard-commons/actions?query=branch%3Amaster+workflow%3A%22Java+CI%22)
[![Publish Release to Maven Central](https://github.com/SDA-SE/sda-dropwizard-commons/workflows/Publish%20Release%20to%20Maven%20Central/badge.svg)](https://github.com/SDA-SE/sda-dropwizard-commons/actions?query=workflow%3A%22Publish+Release+to+Maven+Central%22)
[![Publish Release to SDA](https://github.com/SDA-SE/sda-dropwizard-commons/workflows/Publish%20Release%20to%20SDA/badge.svg)](https://github.com/SDA-SE/sda-dropwizard-commons/actions?query=workflow%3A%22Publish+Release+to+SDA%22)
[![FOSSA Status](https://app.fossa.com/api/projects/custom%2B8463%2Fsda-dropwizard-commons.svg?type=shield)](https://app.fossa.com/reports/2d8b4a40-db62-4c73-a978-588e252aa6e8)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=SDA-SE_sda-dropwizard-commons&metric=alert_status)](https://sonarcloud.io/dashboard?id=SDA-SE_sda-dropwizard-commons)
[![SonarCloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=SDA-SE_sda-dropwizard-commons&metric=coverage)](https://sonarcloud.io/dashboard?id=SDA-SE_sda-dropwizard-commons)
[![SonarCloud Reliability](https://sonarcloud.io/api/project_badges/measure?project=SDA-SE_sda-dropwizard-commons&metric=reliability_rating)](https://sonarcloud.io/component_measures/metric/reliability_rating/list?id=SDA-SE_sda-dropwizard-commons)
[![SonarCloud Security](https://sonarcloud.io/api/project_badges/measure?project=SDA-SE_sda-dropwizard-commons&metric=security_rating)](https://sonarcloud.io/component_measures/metric/security_rating/list?id=SDA-SE_sda-dropwizard-commons)

SDA Dropwizard Commons is a set of libraries to bootstrap services easily that follow the patterns and specifications promoted by
the SDA SE.

>
> ##### ⚠️ ATTENTION: Please use SDA Dropwizard Commons version 2 and newer:
>
> New features can only be contributed in version 2.x.x.
> Version 1.x.x only receives critical security updates.
> Version 1.x.x will be discontinued at the same time Dropwizard version 1.3.x is discontinued. This is [announced for 31.12.2020](https://groups.google.com/g/dropwizard-user/c/6gpDMuSb4_Y).
> Please make sure to upgrade to version 2 as soon as possible.
>

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
- [OpenTracing](https://opentracing.io/)


## Changelog and Versioning

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

See our [changelog](https://github.com/SDA-SE/sda-dropwizard-commons/releases/) for more information about the latest features.


## Contributing

We are looking forward to contributions.
Take a look at our [Contribution Guidelines](./CONTRIBUTING.md) before submitting Pull Requests.


## Responsible Disclosure and Security

The [SECURITY.md](./SECURITY.md) includes information on responsible disclosure and security related topics like security patches.


## Modules in SDA Dropwizard Commons


### Server

All modules prefixed with `sda-commons-server-` provide technology and configuration support used in backend services
to provide REST Endpoints.


#### Main Server Modules

The main server modules help to bootstrap and test a Dropwizard application with convergent dependencies. 


##### Starter

The module [`sda-commons-starter`](./sda-commons-starter/README.md) provides all basics required to build 
a service for the SDA Platform with Dropwizard.

The module [`sda-commons-starter-example`](./sda-commons-starter-example/README.md) gives a small example 
on starting an application using defaults for the SDA Platform.


##### Testing

The module [`sda-commons-server-testing`](./sda-commons-server-testing/README.md) is the base module to add unit and 
integration tests for applications in the SDA SE infrastructure.

Some modules have a more specialized testing module, e.g. the
[`sda-commons-server-hibernate`](./sda-commons-server-hibernate/README.md) module has a 
[`sda-commons-server-hibernate-testing`](./sda-commons-server-hibernate-testing/README.md) module, providing further
support.


#### Additional Server Modules

The additional server modules add helpful technologies to the Dropwizard application. 


##### Auth

The module [`sda-commons-server-auth`](./sda-commons-server-auth/README.md) provides support to add authentication
using JSON Web Tokens with different sources for the public keys of the signing authorities.


##### Circuit Breaker

The module [`sda-commons-server-circuitbreaker`](./sda-commons-server-circuitbreaker/README.md) provides support to 
inject circuit breakers into synchronous calls to other services.


##### Consumer Token

The module [`sda-commons-server-consumer`](./sda-commons-server-consumer/README.md) adds support to track or require a 
consumer token identifying the calling application. 


##### Cross-Origin Resource Sharing

The module [`sda-commons-server-cors`](./sda-commons-server-cors/README.md) adds support for CORS. This allows
Cross-origin resource sharing for the service.


##### Dropwizard

The module [`sda-commons-server-dropwizard`](./sda-commons-server-dropwizard/README.md) provides 
`io.dropwizard:dropwizard-core` with convergent dependencies. All other SDA Dropwizard Commons Server modules use this dependency
and are aligned to the versions provided by `sda-commons-server-dropwizard`. It also provides some common bundles that
require no additional dependencies.


##### Health Check
The module [`sda-commons-server-healthcheck`](./sda-commons-server-healthcheck/README.md) introduces the possibility
to distinguish internal and external health checks.

The module [`sda-commons-server-healthcheck-example`](./sda-commons-server-healthcheck-example/README.md) 
presents a simple application that shows the usage of the bundle and implementation of new health checks. 


##### Hibernate

The module [`sda-commons-server-hibernate`](./sda-commons-server-hibernate/README.md) provides access to relational
databases with hibernate.

The module [`sda-commons-server-hibernate-exmaple`](./sda-commons-server-hibernate-example/README.md) shows how
to use the bundle within an application.


##### Jackson

The module [`sda-commons-server-jackson`](./sda-commons-server-jackson/README.md) is used for several purposes
* configure the `ObjectMapper` with the recommended default settings of SDA SE services. 
* provides support for linking resources with HAL 
* adds the ability to filter fields on client request
* registers exception mapper to support the common error structure as defined within the rest guide


##### Forms

The module [`sda-commons-shared-forms`](./sda-commons-shared-forms/README.md) adds all required dependencies to support 
`multipart/*` in Dropwizard applications.


##### Kafka

The module [`sda-commons-server-kafka`](./sda-commons-server-kafka/README.md) provides means to send and consume 
messages from a Kafka topic.

The module [`sda-commons-server-kafka-example`](./sda-commons-server-kafka-example/README.md) includes 
applications, one with consumer and one with producer examples.   


##### MongoDB

The module [`sda-commons-server-morphia`](./sda-commons-server-morphia/README.md) is used to work
with MongoDB using [Morphia](https://github.com/MorphiaOrg).

The module [`sda-commons-server-mongo-testing`](./sda-commons-server-mongo-testing/README.md) 
provides a MongoDB instance for integration testing.

The module [`sda-commons-server-morphia-exmaple`](./sda-commons-server-morphia-example/README.md) shows how
to use the bundle within an application.


##### Open Tracing

The module [`sda-commons-server-opentracing`](./sda-commons-server-opentracing/README.md) provides [OpenTracing](https://opentracing.io/) instrumentation for JAX-RS. 
Other bundles like `sda-commons-client-jersey`, `sda-commons-server-morphia` or `sda-commons-server-s3` come with built in instrumentation.

Besides instrumentation it's also required to specify a collector, like [Jaeger](https://www.jaegertracing.io/).
The module [`sda-commons-server-jaeger`](./sda-commons-server-jaeger/README.md) provides the Jaeger collector.

The module [`sda-commons-server-opentracing-exmaple`](./sda-commons-server-morphia-example/README.md) shows how to use OpenTracing and Jaeger within an application and has examples for manual instrumentation.


##### Prometheus

The module [`sda-commons-server-prometheus`](./sda-commons-server-prometheus/README.md) provides an admin endpoint to
serve metrics in a format that Prometheus can read.

The module [`sda-commons-server-prometheus-example`](./sda-commons-server-prometheus-example/README.md) 
presents a simple application that shows the three main types of metrics to use in a service. 


##### S3 Object Storage

The module [`sda-commons-server-s3`](./sda-commons-server-s3/README.md) provides a client for an 
AWS S3-compatible object storage.

The module [`sda-commons-server-s3-testing`](./sda-commons-server-s3-testing/README.md) is used to 
provide an [AWS S3-compatible Object Storage](https://docs.aws.amazon.com/AmazonS3/latest/API/Welcome.html) during integrations tests.


##### Security

The module [`sda-commons-server-security`](./sda-commons-server-security/README.md) helps to configure a secure 
Dropwizard application.


##### Swagger

The module [`sda-commons-server-swagger`](./sda-commons-server-swagger/README.md) is the base 
module to add [Swagger](https://github.com/swagger-api/swagger-core) support for applications
in the SDA SE infrastructure.


##### Trace Token

The module [`sda-commons-server-trace`](./sda-commons-server-trace/README.md) adds support to track create a 
trace token to correlate  a set of service invocations that belongs to the same logically cohesive call of a higher 
level service offered by the SDA Platform, e.g. interaction service. . 


##### Weld

The module [`sda-commons-server-weld`](./sda-commons-server-weld/README.md) is used to bootstrap Dropwizard applications 
inside a Weld-SE container and provides CDI support for servlets, listeners and resources.

The module [`sda-commons-server-weld-example`](./sda-commons-server-weld-example/README.md) gives a small example on
starting an application within an Weld container.


##### YAML

The module [`sda-commons-shared-yaml`](./sda-commons-shared-yaml/README.md) adds support for YAML-file handling.


### Client

All modules prefixed with `sda-commons-client-` provide support for applications that use a HTTP client to access other
services.


#### Jersey

The module [`sda-commons-client-jersey`](./sda-commons-client-jersey/README.md) provides support for using Jersey 
clients withing the Dropwizard application.

The module [`sda-commons-client-jersey-wiremock-testing`](./sda-commons-client-jersey-wiremock-testing/README.md) 
bundles the [WireMock](https://wiremock.org) dependencies to mock services in integration tests consistently to 
sda-commons library versions.

The module [`sda-commons-client-jersey-example`](./sda-commons-client-jersey-example/README.md)
presents an example application that shows how to invoke services.


#### Forms

The module [`sda-commons-shared-forms`](./sda-commons-shared-forms/README.md) adds all required dependencies to support 
`multipart/*` in Dropwizard applications.


## Usage

The compiled releases are publicly available via [maven central](https://search.maven.org/search?q=org.sdase.commons).

Include `sda-commons-bom` and `sda-commons-dependencies` as platform constraints. You will inherit 
all versions defined there and won't have to specify versions for them yourself.

More details: 
- [sda-commons-bom](sda-commons-bom/README.md) 
- [sda-commons-dependencies](sda-commons-dependencies/README.md) 

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
