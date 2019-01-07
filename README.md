# SDA Commons

[![Build Status](https://jenkins.intern.sda-se.com/buildStatus/icon?job=SDA%20Open%20Industry%20Solutions/sda-commons/master)](https://jenkins.intern.sda-se.com/job/SDA%20Open%20Industry%20Solutions/job/sda-commons/job/master/)

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

## Changelog and Versioning

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html) and uses [Semantic Commits](https://gist.github.com/stephenparish/9941e89d80e2bc58a153).

Our [changelog](https://github.com/SDA-SE/sda-commons/releases/) is maintained in the GitHub releases.

## Modules in SDA Commons

### Server

All modules prefixed with `sda-commons-server-` provide technology and configuration support used in backend services
to provide REST Endpoints.


#### Main Server Modules

The main server modules help to bootstrap and test a Dropwizard application with convergent dependencies. 

##### Dropwizard

The module [`sda-commons-server-dropwizard`](./sda-commons-server-dropwizard/README.md) provides 
`io.dropwizard:dropwizard-core` with convergent dependencies. All other SDA Commons Server modules use this dependency
and are aligned to the versions provided by `sda-commons-server-dropwizard`. It also provides some common bundles that
require no additional dependencies.

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

##### Consumer Token

The module [`sda-commons-server-consumer`](./sda-commons-server-consumer/README.md) adds support to track or require a 
consumer token identifying the calling application. 

Status:
- Ready to use if the application itself does not act as client
- If the application acts as a client as well, `ConsumerTokenClientFilter` and `ConsumerTokenContainerFilter` from
  [rest-common](#usage-in-combination-with-rest-common) should be used

##### Cross-Origin Resource Sharing

The module [`sda-commons-server-cors`](./sda-commons-server-cors/README.md) adds support for CORS. This allows
Cross-origin resource sharing for the service.

Status:
- Ready to use

##### Hibernate

The module [`sda-commons-server-hibernate`](./sda-commons-server-hibernate/README.md) provides access to relational
databases with hibernate.

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

##### Kafka

The module [`sda-commons-server-kafka`](./sda-commons-server-kafka/README.md) provides means to send and consume 
messages from a kafka topic.

The module [`sda-commons-server-kafka-example`](./sda-commons-server-kafka-example/README.md) includes 
applications, one with consumer and one with producer examples.   

Status:
- Ready to use with JSON messages

##### Prometheus

The module [`sda-commons-server-prometheus`](./sda-commons-server-prometheus/README.md) provides an admin endpoint to
serve metrics in a format that Prometheus can read.

Status:
- Ready to use in combination with [SDA Commons Consumer Token](#consumer-token)
- Metrics will miss the consumer token when used with consumer token support form rest-common 

##### Security

The module [`sda-commons-server-security`](./sda-commons-server-security/README.md) helps to configure a secure 
Dropwizard application.

Status
- Ready to use, but providing only a subset of the 
  "[Härtungsmaßnahmen Dropwizard](https://sda-se.atlassian.net/wiki/spaces/platform/pages/686718998/H+rtungsma+nahmen+Dropwizard)" 
  by Timo Pagel

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


### Client

All modules prefixed with `sda-commons-client-` provide support for applications that use a Http client to access other
services.

#### Jersey

The module [`sda-commons-client-jersey`](./sda-commons-client-jersey/README.md) provides support for using Jersey 
clients withing the dropwizard application.

The module [`sda-commons-client-jersey-wiremock-testing`](./sda-commons-client-jersey-wiremock-testing/README.md) 
bundles the [WireMock]('https://wiremock.org') dependencies to mock services in integration tests consistently to sda-commons library versions.

The module [`sda-commons-client-jersey-example`](./sda-commons-client-jersey-example/README.md)
presents an example application that shows how to invoke services.

Status:
- Ready to use
- When using new threads for clients to invoke another service, the trace token is not transferred out-of-the-box.


## Usage in combination with rest-common

To keep convergent dependencies when some features of [`rest-common`](https://github.com/SDA-SE/rest-common) are needed,
`rest-common` should be added as follows:

```
    // rest-common
    compile('com.sdase.framework:rest-common:0.56.0') {
        exclude group: 'ma.glasnost.orika', module: 'orika-core'
    }

    // add excluded dependency from rest-common with appropriate excludes
    compile ('ma.glasnost.orika:orika-core:1.5.2') {
        exclude group: 'org.slf4j', module: 'slf4j-api'
        exclude group: 'org.javassist', module: 'javassist'
        exclude group: 'com.thoughtworks.paranamer', module: 'paranamer'
    }
```

Convergent dependencies may be forced in the build.gradle:

```
    configurations.all {
        resolutionStrategy {
            failOnVersionConflict()
        }
    }
```
