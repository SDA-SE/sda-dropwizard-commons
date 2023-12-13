# Migration Guide from v5 to v6

## General migration changes

You can find the full release notes in the
official [Dropwizard repository](https://github.com/dropwizard/dropwizard/releases/tag/v4.0.0).
Below we list the most important changes:

### Dropwizard Package Structure and JPMS

In order to properly support the Java Platform Module System (JPMS), the Java packages in modules
must not overlap, or put differently, the packages may not be split into multiple modules.

Affected packages:

| Maven module       | Old package           | New package                  |
|--------------------|-----------------------|------------------------------|
| dropwizard-core    | io.dropwizard         | io.dropwizard.core           |
| dropwizard-logging | io.dropwizard.logging | io.dropwizard.logging.common |
| dropwizard-metrics | io.dropwizard.metrics | io.dropwizard.metrics.common |
| dropwizard-views   | io.dropwizard.views   | io.dropwizard.views.common   |

### Jakarta package namespace

The previous package namespace for Java EE, `javax.*` was replaced by `jakarta` namespace, so it was
necessary to replace all the imports starting with `import javax.` to `import jakarta.` and
dependencies.
Here is the list with the modified dependencies:

- javax.xml.bind:jaxb-api -> jakarta.xml.bind:jakarta.xml.bind-api
- javax.annotation:javax.annotation-api -> jakarta.annotation:jakarta.annotation-api
- javax.transaction:javax.transaction-api -> jakarta.transaction:jakarta.transaction-api
- io.swagger.core.v3:swagger-annotations -> io.swagger.core.v3:swagger-annotations-jakarta
- io.swagger.core.v3:swagger-jaxrs2 -> io.swagger.core.v3:swagger-jaxrs2-jakarta
- io.swagger.core.v3:swagger-annotations -> io.swagger.core.v3:swagger-annotations-jakarta
- io.swagger.core.v3:swagger-core -> io.swagger.core.v3:swagger-core-jakarta

You can find more details about this change
on [Upgrade Notes for Dropwizard 4.0.x](https://www.dropwizard.io/en/latest/manual/upgrade-notes/upgrade-notes-4_0_x.html#transition-to-jakarta-ee).

### Apache Http Client

The Apache version was upgraded from v4 to v5.
The imports were changed from `org.apache.http` to `org.apache.hc.core5`
and `org.apache.hc.client5`.
You can check the full migration guide in the
official
documentation: [Migration from Apache HttpClient 4.x APIs](https://hc.apache.org/httpcomponents-client-5.2.x/migration-guide/preparation.html)

**Closing Responses**

The Apache 5 connector seems to be more sensitive and might get stuck if you don't
close your `Response` objects. Make sure to use *try-with-resources* or
`finally` when you use Jersey clients (either in tests or in production!).

Example:

```java
try (Response response = DW.client()
    .target("http://localhost:" + DW.getLocalPort())
    .path("/example")
    .request(APPLICATION_JSON)
    .get()) {
  assertThat(response.getStatus()).isEqualTo(200);
}
```

### Jetty 11

Dropwizard v4 upgraded to Jetty 11.0.x. The main changes were regarding supporting `jakarta.servlet`
namespace and a complete WebSocket refactoring, those using the Jetty APIs or embedded-jetty will
need to update their code.
You can read more information in
the [release notes](https://github.com/jetty/jetty.project/releases/tag/jetty-11.0.0) and in
the [official migration guide](https://eclipse.dev/jetty/documentation/jetty-11/programming-guide/index.html#pg-migration-94-to-10).

### Hibernate 6
The Hibernate library was upgraded to 6.1. Both of them provide compatible implementations for Jakarta Persistence 3.0.
You can check the migration guide
to [v6.0](https://github.com/hibernate/hibernate-orm/blob/6.0/migration-guide.adoc#60-migration-guide)
and
to [v6.1](https://github.com/hibernate/hibernate-orm/blob/6.1/migration-guide.adoc#61-migration-guide).

## Modules
The following modules contain changes:

1. [sda-commons-server-testing](#1-sda-commons-server-testing)
2. [sda-commons-server-spring-data-mongo](#2-sda-commons-server-spring-data-mongo)
3. [sda-commons-server-mongo-testing](#3-sda-commons-server-mongo-testing)
4. [sda-commons-client-jersey-wiremock-testing](#4-wiremock-3.0)
5. [sda-commons-server-circuitbreaker](#5-sda-commons-server-circuitbreaker)
6. [sda-commons-shared-asyncapi](#6-sda-commons-shared-asyncapi)
7. [sda-commons-server-kafka](#7-sda-commons-server-kafka)
8. [sda-commons-server-weld-testing](#8-sda-commons-server-weld-testing)
9. [sda-commons-server-s3](#9-sda-commons-server-s3)
10. [sda-commons-server-s3-testing](#10-sda-commons-server-s3-testing)
11. [sda-commons-server-prometheus](#11-sda-commons-server-prometheus)

### 1 sda-commons-server-testing

Removed Support for JUnit 4.x
You must use all JUnit 5 extensions, classes, annotations, and libraries and migrate all your JUnit
4 tests to JUnit 5.

### 2 sda-commons-server-spring-data-mongo

The deprecated legacy configuration support for individual properties like `hosts` or `database` was
removed. The database connection must be configured with [`connectionString`](https://www.mongodb.com/docs/manual/reference/connection-string/).

### 3 sda-commons-server-mongo-testing

Removed custom proxy configuration for MongoDB executable download.
OS proxy settings should be configured instead.

#### FixtureHelpers

The class `io.drowizard.helpers.fixtures.FixtureHelpers` is not available in Dropwizard v4. So
you must read the file using other approaches, e.g.
using [Wiremock response body](https://wiremock.org/docs/stubbing/#specifying-the-response-body) or
using an [ObjectMapper](https://www.baeldung.com/jackson-object-mapper-tutorial).

### 4 sda-commons-client-jersey-wiremock-testing

Dropwizard v4 uses wiremock v3.x version. Were introduced some breaking changes, like dropping
support for Java 8,
upgrading from Jetty 9 to Jetty 11 and changing the repository groupID to org.wiremock for all
artifacts : wiremock, wiremock-standalone, wiremock-webhooks-extension.
Module `sda-commons-client-jersey-wiremock-testing` was renamed to `sda-commons-shared-wiremock-testing`.
SDA specific Wiremock test extensions were removed and replaced with Wiremock internal extensions.
You will find example tests in [WireMockExampleTest](https://github.com/SDA-SE/sda-dropwizard-commons/blob/69f1b0a7158d3ee6c1e0a55d63f77bd3282a6839/sda-commons-shared-wiremock-testing/src/test/java/org/sdase/commons/shared/wiremock/testing/WireMockExampleTest.java).

You can see all the release notes and breaking changes in
the [official repository](https://github.com/wiremock/wiremock/releases/3.0.0). 

### 5 sda-commons-server-circuitbreaker

Resilience4j-Circuitbreaker was updated from 1.7.x to 2.1.
Please check [their release notes](https://github.com/resilience4j/resilience4j/blob/master/RELEASENOTES.adoc#version-200) for details.

The class `org.sdase.commons.server.circuitbreaker.metrics.SdaCircuitBreakerMetricsCollector` was removed. 
We now collect metrics using [Micrometer](https://micrometer.io/).

The metric named `resilience4j_circuitbreaker_calls_bucket` is not exposed anymore.
Please use Micrometer's metric `resilience4j_circuitbreaker_calls_count` instead.

### 6 sda-commons-shared-asyncapi

Json Schemas for AsyncAPI are generated with
[Victools' Json Schema Generator](https://github.com/victools/jsonschema-generator) now.
The [previously used library](https://github.com/mbknor/mbknor-jackson-jsonSchema) is barely
maintained in the past years.

The old library provided their own annotations.
Now, annotations of Jackson (e.g. `@JsonSchemaDescription`), Swagger (e.g. `@Schema`) and Jakarta
Validation (e.g. `NotNull`) can be used.
Note that not all attributes of all annotations are covered and multiple examples are not possible
anymore.
Only one example can be defined with `@Schema(example = "value")`.

How the Java classes for schema definitions in the AsyncAPI are defined has changed.
Previously, classes to integrate were defined in the code
(`.withSchema("./schema.json", BaseEvent.class)`) and referenced in the AsyncAPI template
(`$ref: './schema.json#/definitions/CarManufactured'`).
Now the classes are referenced directly in the template (`$ref: 'class://com.example.BaseEvent`).
The builder method `withSchema` does not exist anymore.

Please review the differences in the generated AsyncAPI file.
Both libraries work different and have a different feature set.
The new generator may have some limitations but a great API for extensions.
Please [file an issue](https://github.com/SDA-SE/sda-dropwizard-commons/issues) if something
important can't be expressed.


### 7 sda-commons-server-kafka

### 7 Kafka

The public init method

`MessageListenerStrategy#init(ConsumerTopicMessageHistogram consumerTopicMessageHistogram, Set<String> metadataFields)`

was removed. Internal metrics are collected automatically. Please use

`MessageListenerStrategy#init(Set<String> metadataFields)`


### 8 sda-commons-server-weld-testing

The custom JUnit extensions have been removed. Please make use of the
official [JUnit5 extension](https://github.com/weld/weld-testing/tree/master/junit5), which this
module is now providing. An example how
to use it can be found in [sda-commons-server-weld-example](../../sda-commons-server-weld-example).

### 9 sda-commons-server-s3

The AWS SDK was upgraded from 1.12.x to https://github.com/aws/aws-sdk-java-v2.
The new version is not compatible with the old one.
You can find the official documentation for the migration https://github.com/aws/aws-sdk-java-v2/blob/master/docs/LaunchChangelog.md.

This can also be explained by the Jakarta update because the old AWS SDK still used the Apache
HTTP Client v4, which is not compatible with Jakarta.
The old AWS SDK did not have the option to pick a different HTTP client.

Most noticeably, the base package of the classes moved from `com.amazonaws` to `software.amazon.awssdk`.
The `S3Bundle` will not return an instance of `software.amazon.awssdk.services.s3.S3Client`
instead of `com.amazonaws.services.s3.AmazonS3`

Moreover, the bundle now supports using the AWS [default credentials provider](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/auth/credentials/DefaultCredentialsProvider.html) 
chain to retrieve credentials if you don't provide them in the `S3Configuration`.
You can now also pass credentials
via environment variables `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`
or via system properties `aws.accessKeyId` and `aws.secretAccessKey`.

### 10 sda-commons-server-s3-testing

Our previous S3 mock library [s3mock](https://github.com/Robothy/local-s3) was also based on the old 
AWS SDK v1.12.x and is no longer maintained. 
We switched to [Robothy local-s3](https://github.com/Robothy/local-s3) as alternative.
You can still use our `S3ClassExtension` to start the S3 mock server in your tests.
But it will now give you an instance of `software.amazon.awssdk.services.s3.S3Client` for the
S3 client.
Additionally, you need to annotate your S3 tests with `@LocalS3` due to an implementation detail
of the underlying test library.

### 11 sda-commons-server-prometheus

The endpoint `/healthcheck/prometheus` was removed. Health checks metrics are available using endpoint `/metrics/prometheus`.

SDA specific Prometheus metrics were removed.

## Automation

The following bash script can help you to quickly migrate your project to sda-dropwizard-commons 6.
Copy the content to a file in the root of your project and execute it.

```bash
touch migrate.sh
chmod u+x migrate.sh
````

```bash
#!/bin/bash

echo "Migrating Java files"
for file in `find . -type f -name "*.java"`
do
  echo "Processing $file"
  sed -i '' -e 's/javax.validation/jakarta.validation/g' $file
  sed -i '' -e 's/javax.ws.rs/jakarta.ws.rs/g' $file
  sed -i '' -e 's/javax.annotation/jakarta.annotation/g' $file
  sed -i '' -e 's/io.dropwizard.Application/io.dropwizard.core.Application/g' $file
  sed -i '' -e 's/io.dropwizard.Configuration/io.dropwizard.core.Configuration/g' $file
  sed -i '' -e 's/io.dropwizard.setup.Bootstrap/io.dropwizard.core.setup.Bootstrap/g' $file
  sed -i '' -e 's/io.dropwizard.setup.Environment/io.dropwizard.core.setup.Environment/g' $file
  sed -i '' -e 's/com.amazonaws.services.s3.AmazonS3/software.amazon.awssdk.services.s3.S3Client/g' $file
  sed -i '' -e 's/AmazonS3/S3Client/g' $file

  # test files only
  if [[ $file =~ .*src\/test.* ]]; then
      sed -i '' -e 's/org.sdase.commons.client.jersey.wiremock.testing.WireMockClassExtension/com.github.tomakehurst.wiremock.junit5.WireMockExtension/g' $file
      sed -i '' -e 's/new WireMockClassExtension(wireMockConfig().dynamicPort());/new WireMockExtension().builder().build()/g' $file
      sed -i '' -e 's/WireMockClassExtension;/WireMockExtension/g' $file
  fi
done

echo "Migrating Gradle files"
for file in `find . -type f -name "build.gradle"`
do
  echo "Processing $file"
  sed -i '' -e 's/org.sdase.commons:sda-commons-client-jersey-wiremock-testing/org.sdase.commons:sda-commons-shared-wiremock-testing/g' $file
done
```