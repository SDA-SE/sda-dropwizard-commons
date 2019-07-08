
[![Build Status](https://jenkins.intern.sda-se.com/buildStatus/icon?job=SDA%20Open%20Industry%20Solutions/sda-commons/master)](https://jenkins.intern.sda-se.com/job/SDA%20Open%20Industry%20Solutions/job/sda-commons/job/master/)
[![FOSSA Status](https://app.fossa.com/api/projects/custom%2B8463%2Fsda-commons.svg?type=shield)](https://app.fossa.com/projects/custom%2B8463%2Fsda-commons?ref=badge_shield)

SDA Commons is a set of libraries to bootstrap services easily that follow the patterns and specifications promoted by
the SDA SE.

SDA Commons is separated in different modules that can be combined as needed. Most of the modules require the 
technologies that are recommended for services in the SDA SE Platform. These technologies include

- [Dropwizard](https://www.dropwizard.io)
- [Jackson](https://github.com/FasterXML/jackson)
- [JAX-RS](https://jcp.org/en/jsr/detail?id=339) 
- [Jersey](https://jersey.github.io/)
- [Swagger](https://swagger.io/)
- [Hibernate](http://hibernate.org/)
- [Kafka](https://kafka.apache.org/)
- [MongoDB](https://www.mongodb.com)

## Changelog and Versioning

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html) and uses 
[Semantic Commits](https://gist.github.com/stephenparish/9941e89d80e2bc58a153).

Our [changelog](https://github.com/SDA-SE/sda-commons/releases/) is maintained in the GitHub releases.

## Modules in SDA Commons

### Server

All modules prefixed with `sda-commons-server-` provide technology and configuration support used in backend services
to provide REST Endpoints.


#### Main Server Modules

The main server modules help to bootstrap and test a Dropwizard application with convergent dependencies. 

##### Starter

The module [`sda-commons-server-starter`](./sda-commons-server-starter/README.md) provides all basics required to build 
a service for the SDA Platform with Dropwizard.

The module [`sda-commons-server-starter-example`](./sda-commons-server-starter-example/README.md) gives a small example 
on starting an application using defaults for the SDA Platform.

Status:
- Ready to use

##### Testing

The module [`sda-commons-server-testing`](./sda-commons-server-testing/README.md) is the base module to add unit and 
integration tests for applications in the SDA SE infrastructure.

Some modules have a more specialized testing module, e.g. the
[`sda-commons-server-hibernate`](./sda-commons-server-hibernate/README.md) module has a 
[`sda-commons-server-hibernate-testing`](./sda-commons-server-hibernate-testing/README.md) module, providing further
support.

The module [`sda-commons-server-kafka-confluent-testing`](./sda-commons-server-kafka-confluent-testing/README.md),
provides support to start a confluent schema registry needed if you use Avro with the confluent serializers. 

Status:
- Ready to use

#### Additional Server Modules

The additional server modules add helpful technologies to the Dropwizard application. 

##### Auth

The module [`sda-commons-server-auth`](./sda-commons-server-auth/README.md) provides support to add authentication
using JSON Web Tokens with different sources for the public keys of the signing authorities.

Status:
- Ready to use

##### Circuit Breaker

The module [`sda-commons-server-circuitbreaker`](./sda-commons-server-circuitbreaker/README.md) provides support to 
inject circuit breakers into synchronous calls to other services.

Status:
- Ready to use

##### Consumer Token

The module [`sda-commons-server-consumer`](./sda-commons-server-consumer/README.md) adds support to track or require a 
consumer token identifying the calling application. 

Status:
- Ready to use

##### Cross-Origin Resource Sharing

The module [`sda-commons-server-cors`](./sda-commons-server-cors/README.md) adds support for CORS. This allows
Cross-origin resource sharing for the service.

Status:
- Ready to use

##### Dropwizard

The module [`sda-commons-server-dropwizard`](./sda-commons-server-dropwizard/README.md) provides 
`io.dropwizard:dropwizard-core` with convergent dependencies. All other SDA Commons Server modules use this dependency
and are aligned to the versions provided by `sda-commons-server-dropwizard`. It also provides some common bundles that
require no additional dependencies.

Status:
- Ready to use

##### Healthcheck
The module [`sda-commons-server-healthcheck`](./sda-commons-server-healthcheck/README.md) introduces the possibility
to distinguish internal and external health checks.

The module [`sda-commons-server-healthcheck-example`](./sda-commons-server-healthcheck-example/README.md) 
presents a simple application that shows the usage of the bundle and implementation of new health checks. 

Status:
- Ready to use

##### Hibernate

The module [`sda-commons-server-hibernate`](./sda-commons-server-hibernate/README.md) provides access to relational
databases with hibernate.

The module [`sda-commons-server-hibernate-exmaple`](./sda-commons-server-hibernate-example/README.md) shows how
to use the bundle within an application.

Status:
- Ready to use

##### Jackson

The module [`sda-commons-server-jackson`](./sda-commons-server-jackson/README.md) is used for several purposes
* configure the `ObjectMapper` with the recommended default settings of SDA SE services. 
* provides support for linking resources with HAL 
* adds the ability to filter fields on client request
* registers exception mapper to support the common error structure as defined within the rest guide

Status:
- Ready to use

##### Forms

The module [`sda-commons-shared-forms`](./sda-commons-shared-forms/README.md) adds all required dependencies to support 
`multipart/*` in Dropwizard applications.

##### Kafka

The module [`sda-commons-server-kafka`](./sda-commons-server-kafka/README.md) provides means to send and consume 
messages from a kafka topic.

The module [`sda-commons-server-kafka-example`](./sda-commons-server-kafka-example/README.md) includes 
applications, one with consumer and one with producer examples.   

The module [`sda-commons-server-kafka-confluent`](./sda-commons-server-kafka-confluent/README.md) is 
the base module to add Avro specific support to Kafka.

Status:
- Ready to use with JSON messages

##### MongoDB

The module [`sda-commons-server-morphia`](./sda-commons-server-morphia/README.md) is used to work
with MongoDB using [Morphia](https://github.com/MorphiaOrg).

The module [`sda-commons-server-mongo-testing`](./sda-commons-server-mongo-testing/README.md) 
provides a MongoDB instance for integration testing.

The module [`sda-commons-server-morphia-exmaple`](./sda-commons-server-morphia-example/README.md) shows how
to use the bundle within an application.

Status:
- Ready to use


##### Prometheus

The module [`sda-commons-server-prometheus`](./sda-commons-server-prometheus/README.md) provides an admin endpoint to
serve metrics in a format that Prometheus can read.

The module [`sda-commons-server-prometheus-example`](./sda-commons-server-prometheus-example/README.md) 
presents a simple application that shows the three main types of metrics to use in a service. 

Status:
- Ready to use for custom metrics
- Ready to use in combination with [SDA Commons Consumer Token](#consumer-token) or when provided by 
  [SDA Commons Server Starter](#starter) for built in request duration metrics

##### S3 Object Storage

The module [`sda-commons-server-s3`](./sda-commons-server-s3/README.md) provides a client for an 
AWS S3-compatible object storage.

The module [`sda-commons-server-s3-testing`](./sda-commons-server-s3-testing/README.md) is used to 
provide an [AWS S3-compatible Object Storage](https://docs.aws.amazon.com/AmazonS3/latest/API/Welcome.html) during integrations tests.

Status:
- Ready to use


##### Security

The module [`sda-commons-server-security`](./sda-commons-server-security/README.md) helps to configure a secure 
Dropwizard application.

Status
- Ready to use, but providing only a subset of the "Härtungsmaßnahmen Dropwizard" available at the internal wiki entry 
  written by Timo Pagel

##### Swagger

The module [`sda-commons-server-swagger`](./sda-commons-server-swagger/README.md) is the base 
module to add [Swagger](https://github.com/swagger-api/swagger-core) support for applications
in the SDA SE infrastructure.

Status:
- Ready to use

##### Trace Token

The module [`sda-commons-server-trace`](./sda-commons-server-trace/README.md) adds support to track create a 
trace token to correlate  a set of service invocations that belongs to the same logically cohesive call of a higher 
level service offered by the SDA platform, e.g. interaction service. . 

Status:
- Ready to use
- When using new threads for clients to invoke another service, the trace token is not transferred out-of-the-box. 
  The same holds for mentioning the trace token in log entries of new threads.

##### Weld

The module [`sda-commons-server-weld`](./sda-commons-server-weld/README.md) is used to bootstrap Dropwizard applications 
inside a Weld-SE container and provides CDI support for servlets, listeners and resources.

The module [`sda-commons-server-weld-example`](./sda-commons-server-weld-example/README.md) gives a small example on
starting an application within an Weld container.

Status:
- Ready to use

##### YAML

The module [`sda-commons-shared-yaml`](./sda-commons-shared-yaml/README.md) adds support for YAML-file handling.


### Client

All modules prefixed with `sda-commons-client-` provide support for applications that use a Http client to access other
services.

#### Jersey

The module [`sda-commons-client-jersey`](./sda-commons-client-jersey/README.md) provides support for using Jersey 
clients withing the dropwizard application.

The module [`sda-commons-client-jersey-wiremock-testing`](./sda-commons-client-jersey-wiremock-testing/README.md) 
bundles the [WireMock]('https://wiremock.org') dependencies to mock services in integration tests consistently to 
sda-commons library versions.

The module [`sda-commons-client-jersey-example`](./sda-commons-client-jersey-example/README.md)
presents an example application that shows how to invoke services.

Status:
- Ready to use
- When using new threads for clients to invoke another service, the trace token is not transferred out-of-the-box.

#### Forms

The module [`sda-commons-shared-forms`](./sda-commons-shared-forms/README.md) adds all required dependencies to support 
`multipart/*` in Dropwizard applications.

## Usage

Import SDA Commons from the repository `https://nexus.intern.sda-se.online/repository/sda-se-public/` by adding it to the
`build.gradle`:

```
    repositories {
      ...
      maven {
        url "https://nexus.intern.sda-se.online/repository/sda-se-public/"
        credentials {
          username sdaNexusUser
          password sdaNexusPassword
        }
      }
      ...
    }
```

Select and import the required dependencies. Please make sure to always use the same version across modules.
Using a variable for the version is a good practice:

```
    project.ext {
        sdaCommonsVersion = 'x.x.x'
    }

    dependencies {
      ...
      compile "org.sdase.commons:sda-commons-client-jersey:${sdaCommonsVersion}"
      ...
    }
```

### PR Snapshots

Each PR creates a snapshot that can _temporarily_ be included in other projects for testing. The generated version uses
the format: PR-<pr_number>-SNAPSHOT. Snapshots are cleaned up regularly from the repository so never use snapshots in
stable releases.

Import snapshots by adding the snapshot repository to the build.gradle:

```
    repositories {
      ...
      maven {
        url "https://nexus.intern.sda-se.online/repository/sda-se-snapshots/"
        credentials {
          username sdaNexusUser
          password sdaNexusPassword
        }
      }
      ...
    }
```

Add to the dependencies (example):

```
    project.ext {
        sdaCommonsVersion = 'PR-1-SNAPSHOT'
    }
    dependencies {
      ...
      compile "org.sdase.commons:sda-commons-client-jersey:${sdaCommonsVersion}"
      ...
    }
```
