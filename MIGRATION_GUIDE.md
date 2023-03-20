# Migration Guide from v2 to v3

The following modules contain changes:

1. [sda-commons-server-testing](#1-sda-commons-server-testing)
2. [sda-commons-server-auth](#2-sda-commons-server-auth)
3. [sda-commons-server-auth-testing](#3-sda-commons-server-auth-testing)
4. [sda-commons-server-opentracing](#4-sda-commons-server-opentracing)
5. [sda-commons-server-morphia](#5-sda-commons-server-morphia)
6. [sda-commons-server-kafka](#6-sda-commons-server-kafka)
7. [Deployment and Release of upgraded services](#7-deployment-and-release-of-upgraded-services)

## 1 sda-commons-server-testing

Does not provide any JUnit 4 rules anymore.
You should find JUnit 5 extensions for all of your rules.
We recommend migrating all your JUnit 4 tests to JUnit 5.

## 2 sda-commons-server-auth

The deprecated field `readTimeout` was removed from the class `OpaConfig`.
Please set the timeout in the `opaClient` configuration instead.

Example:

```yaml
opa:
  opaClient:
    timeout: 500ms
```

## 3 sda-commons-server-auth-testing

Please change your `test-config.yaml` if they use `${AUTH_RULE}` as placeholder.
We wanted to get rid of all references to old JUnit 4 rules.

```diff
-   config: ${AUTH_RULE}
+   config: ${AUTH_CONFIG_KEYS}
```

## 4 sda-commons-server-opentracing

Migrate from OpenTracing to OpenTelemetry.

### Starter Bundle
If you do not use sda-commons-starter with [SdaPlatformBundle](./sda-commons-starter/src/main/java/org/sdase/commons/starter/SdaPlatformBundle.java),
you need to remove the Jaeger bundle and OpenTracing bundle and add the OpenTelemetry bundle.

```diff
- bootstrap.addBundle(JaegerBundle.builder().build());
- bootstrap.addBundle(OpenTracingBundle.builder().build());
+ bootstrap.addBundle(
+     OpenTelemetryBundle.builder()
+       .withAutoConfiguredTelemetryInstance()
+       .withExcludedUrlsPattern(Pattern.compile(String.join("|", Arrays.asList(
+         "/ping", "/healthcheck", "/healthcheck/internal", "/metrics",
+         "/metrics/prometheus"))))
+       .build());
```

### Replace OpenTracing dependencies to OpenTelemetry dependencies

```diff
- testImplementation 'io.opentracing:opentracing-mock'
+ testImplementation 'io.opentelemetry:opentelemetry-sdk-testing'
```

### Executors need to be instrumented to follow traces when parts of the request are handled asynchronously.

```diff
- return new AsyncEmailSendManager(
-   new InMemoryAsyncTaskRepository<>(limits.getMaxAttempts()),
-   environment
-       .lifecycle()
-       .executorService("email-sender-%d")
-       .minThreads(1)
-       .maxThreads(limits.getMaxParallel())
-       .build(),
+ return new AsyncEmailSendManager(
+     new InMemoryAsyncTaskRepository<>(limits.getMaxAttempts()),
+     io.opentelemetry.context.Context.taskWrapping(
+       environment
+           .lifecycle()
+           .executorService("email-sender-%d")
+           .minThreads(1)
+           .maxThreads(limits.getMaxParallel())
+           .build()),
```

### Environment variables

To disable tracing, you must set the variable `TRACING_DISABLED` to `true`.
The legacy Jaeger environment variables are still supported, but they will be removed in later
versions.

```diff
- JAEGER_SAMPLER_TYPE=const
- JAEGER_SAMPLER_PARAM=0
+ TRACING_DISABLED=true
```

### New environment variables

In order to configure Open Telemetry, you can set some environment variables:

| Name                          | Default value                                 | Description                                                                      |
|-------------------------------|-----------------------------------------------|----------------------------------------------------------------------------------|
| OTEL_PROPAGATORS              | jaeger,b3,tracecontext,baggage                | Propagators to be used as a comma-separated list                                 |
| OTEL_SERVICE_NAME             | value from env `JAEGER_SERVICE_NAME` (if set) | The service name that will appear on tracing                                     |
| OTEL_EXPORTER_JAEGER_ENDPOINT | http://jaeger-collector.jaeger:14250          | Full URL of the Jaeger HTTP endpoint. The URL must point to the jaeger collector |

A full list of the configurable properties can be found in the [General SDK Configuration](https://opentelemetry.io/docs/reference/specification/sdk-environment-variables/).

### New API for instrumentation

If you use the OpenTracing API for manual instrumentation, you will have to replace it with the
OpenTelemetry API.
You can find the [API Definition](https://www.javadoc.io/static/io.opentelemetry/opentelemetry-api/1.0.1/io/opentelemetry/api/GlobalOpenTelemetry.html)
to see all the possible methods.

```diff
- public void sendEmail(To recipient, String subject, EmailBody body) {
-   Span span = GlobalTracer.get().buildSpan("sendSmtpMail").start();
-   try (Scope ignored = GlobalTracer.get().scopeManager().activate(span)) {
-     delegate.sendEmail(recipient, subject, body);
-   } finally {
-     span.finish();
-   }
- }
+ public void sendEmail(To recipient, String subject, EmailBody body) {
+   var tracer = GlobalOpenTelemetry.get().getTracer(getClass().getName());
+   var span = tracer.spanBuilder("sendSmtpMail").startSpan();
+   try (var ignored = span.makeCurrent()) {
+     delegate.sendEmail(recipient, subject, body);
+   } finally {
+     span.end();
+   }
+ }
```

### Test Setup

- Add the opentelemetry test dependency
  ```gradle
  testImplementation 'io.opentelemetry:opentelemetry-sdk-testing' 
  ```

- Replace `io.opentracing.mock.MockTracer` with
  `io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension`

    ```diff
    - private final MockTracer mockTracer = new MockTracer();
    + @RegisterExtension
    + @Order(0)
    + static final OpenTelemetryExtension OTEL = OpenTelemetryExtension.create();
    ```
- Clear metrics and spans before each test using OpenTelemetryExtension

  ```diff
  - @BeforeEach
  - void setupTestData() throws FolderException {
  -   GlobalTracerTestUtil.setGlobalTracerUnconditionally(mockTracer);
  -   mockTracer.reset();
  - }
  + @BeforeEach
  + void setupTestData() throws FolderException {
  +   OTEL.clearMetrics(); // OTEL is the OpenTelemetryExtension instance
  +   OTEL.clearSpans();
  + }
  ```

- Capture spans using `OpenTelemetryExtension` and `io.opentelemetry.sdk.trace.data.SpanData`

    ```diff
    - assertThat(mockTracer.finishedSpans().stream()
    -     .filter(s -> s.operationName().equals("expectedTracing")));
    + assertThat(OTEL.getSpans().stream().filter(s -> s.getName().equals("expectedTracing")));
    ```

## 5 sda-commons-server-morphia

Migrate to [Spring Data Mongo](https://docs.spring.io/spring-data/mongodb/docs/).

The spring-data-mongo package is available at the following coordinates:

```groovy
implementation "org.sdase.commons:sda-commons-server-spring-data-mongo"
```

### Replacing the MorphiaBundle:

```diff
-  private final MorphiaBundle<AppConfiguration> morphiaBundle =
-    MorphiaBundle.builder()
-      .withConfigurationProvider(AppConfiguration::getMongo)
-      .withEntityScanPackageClass(ConsentConfigurationEntity.class)
-      .withValidation()
-      .build();

+  private final SpringDataMongoBundle<AppConfiguration> mongoBundle = 
+    SpringDataMongoBundle.builder()
+      .withConfigurationProvider(AppConfiguration::getMongo)
+      .build();
```

Morphia's `Datastore` can be replaced by Spring Data Mongo's `MongoOperations`:

```java
public MongoOperations getMongoOperations() {
  return this.mongoBundle.getMongoOperations();
}
```

### Mongo Configuration Options

The database connection can be configured in the `config.yaml` of the application.
Please consult the [README of the Spring Data Mongo module](./sda-commons-server-spring-data-mongo/README.md#Configuration)
for further information on how to configure your database connection.

> Please note that we now prefer to configure the MongoDB connection using MongoDB's connection
> string.
> All other configuration options like hosts, options, etc. are still available but deprecated and
> will be removed in the next major release. 
> There is only one exception: The deprecated option `caCertificate` was removed.
> For more information how to mount a certificate please read the [module's documentation](./sda-commons-server-spring-data-mongo/README.md#ca-certificates-support).

### Morphia compatibility

When upgrading a service from 2.x.x a set of converters is provided to stay compatible with a
database that was initialized with Morphia.
To enable these converters, `.withMorphiaCompatibility()` can be used when building the
`SpringDataMongoBundle`.

```java
SpringDataMongoBundle.builder()
  .withConfigurationProvider(AppConfiguration::getSpringDataMongo)
  .withMorphiaCompatibility()
  build();
```

### Validation

Automatic JSR validation is no longer provided in v3.
If you still want to validate your models you can do so manually by using the `ValidationFactory`:

```java
boolean isValid =
  Validation.buildDefaultValidatorFactory()
    .getValidator()
    .validate(myEntity)
    .isEmpty();
```

### Queries

### Building queries with the datastore

Simple operations can be realised by the MongoOperations directly.

Saving an entity:

```java
public void save(SampleEntity entity){
  mongoOperations.insert(entity);
}
```

Find by id:

```java
public Optional<SampleEntity> findById(String id){
  return Optional.ofNullable(mongoOperations.findById(id, SampleEntity.class));
}
```

More complex queries can be realised by building a compound `Query`:

```java
public Optional<SampleEntity> findByArbitraryFields(String identifier, String value) {
  return Optional.ofNullable(
    mongoOperations.findOne(
      new Query(
        where(EntityFields.ARBITRARY_FIELD).is(identifier))
          .and(EntityFields.OTHER_FIELD).ne(value),
        SampleEntity.class));
}
```

See the [official documentation](https://docs.spring.io/spring-data/mongodb/docs/current/api/org/springframework/data/mongodb/core/query/Criteria.html)
for further information.

### Auto-generate queries out of method names with the `MongoRepository` interface
Depending on the complexity, queries can also be auto-generated based on the method names by using
the MongoRepository Interface provided by spring-data-mongo.

Imagine the following simple entity class:

```java
public class Person {
  private String Name;
  private int age;
}
```

Now we can define queries directly through the method names (see the official documentation for the
specific syntax).

```java
public interface PersonMongoRepository extends MongoRepository<Person, String> {
  Optional<Person> findByName(String name);

  List<Person> findAllByAgeIsLessThanEqual(int age);

  List<Person> findAllByNameIsNot(String name);
}
```

See the [official documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.details) for further details.

### Query functions

As Spring Data Mongo doesn't support/provide many query functions provided by Morphia, below are
some replacements/alternatives.

#### EqualIgnoreCase

Morphia supports usage of _equalIgnoreCase()_.
Use _regex()_ in Spring Data Mongo.
For example

- Morphia - `query.criteria("fieldName").equalIgnoreCase(entity.getFieldName());`
- Spring Data Mongo - `query.addCriteria(where("fieldName").regex(Pattern.compile("^" + Pattern.quote(entity.getFieldName()), Pattern.CASE_INSENSITIVE)));`

#### HasAnyOf  

Morphia supports _hasAnyOf()_ method. Use _in()_ in Spring Data Mongo.
For example

- Morphia - `query.field("id").hasAnyOf(getIds());`
- Spring Data Mongo - `query.addCriteria(where("id").in(getIds()));`

#### Filter

Morphia supports _filter()_ method.
Use _is()_ in Spring Data Mongo.
For example

- Morphia - `query.filter("id", getId());`
- Spring Data Mongo - `query.addCriteria(where("id").is(getId()));`

#### Contains

Morphia supports _contains()_ method.
Use _regex()_ in Spring Data Mongo.
For example

- Morphia - `query.criteria("fieldName").contains(entity.getFieldName());`
- Spring Data Mongo - `query.addCriteria(where("fieldName").regex(Pattern.compile(".*" + Pattern.quote(entity.getFieldName()) + ".*")));`

#### HasAllOf

Morphia supports _contains()_ method.
Use _regex()_ in Spring Data Mongo.
For example

- Morphia - `query.field("fieldName").hasAllOf(getFieldNameValues());`
- Spring Data Mongo - `query.addCriteria(where("fieldName").all(getFieldNameValues()));`

### Error handling

Morphia raised `ValidationException`s or subclasses of it when validation errors like duplicate
unique keys occur at database level.
Spring Data Mongo will throw more specific exceptions like
`org.springframework.dao.DuplicateKeyException` or more generic
`org.springframework.dao.DataAccessException`.
Error handling must be updated.

### Indexes

While all `@Indexes` can be created at the main entity class in Morphia, each field must be
`@Indexed`, `@TextIndexed` or `@WildcardIndexed` where it is defined in Spring Data Mongo.
A `@CompoundIndex` can be declared at the entity class.

**It is important to set the index name in the annotations to match the previous name created by
Morphia.**
MongoDB makes it quite difficult to rename an index.
It is highly recommended validating the indexes that are generated by Spring Data MongoDB in
comparison with the existing indexes of a database populated with the latest version of a service.

### Custom converters

Custom converters can be added directly to the Entity class in Morphia by using annotation like
`@Converters(value = {ExampleConverter.class})`.
In Spring Data Mongo the custom converters can be added to the builder in your `Application.class`
like below:

```java
SpringDataMongoBundle.builder()
  .withConfigurationProvider(AppConfiguration::getSpringDataMongo)
  .withEntities(ExampleClass.class)
  .addCustomConverters(
    new ExampleReadingConverter(),
    new ExampleWritingConverter())
  build();
```

Implementing the converter changes from Morphia interface to using the Spring interface can be done
like here [ZonedDateTimeReadConverter.java](sda-commons-server-spring-data-mongo/src/main/java/org/sdase/commons/server/spring/data/mongo/converter/ZonedDateTimeReadConverter.java).

### Annotations

| Morphia                                                   | Spring Data Mongo                                                                                                                                                                                                                           |
|-----------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `@Entity(noClassnameStored = true, name="exampleEntity")` | `@Document("exampleEntity")`. There is no property similar to _noClassnameStored_ as the type/class can't be excluded with Spring Data.                                                                                                     |
| `@PrePersist`                                             | There is no replacement annotation for this. If you are using this on any fields, please set the field before save(). One very common example is to set the creation date like this `entity.setCreated(ZonedDateTime.now(ZoneOffset.UTC));` |
| `@Embedded`                                               | No replacement available as Spring Data already embeds the document.                                                                                                                                                                        |
| `@Converters()`                                           | Replaced with `@ReadingConverter` and `@WritingConverter`                                                                                                                                                                                   |


## 6 sda-commons-server-kafka

Some deprecated code was removed.
Especially we removed the feature `createTopicIfMissing` because we don't recommend services
to do that.
You usually need different privileges for topic creation that you don't want to give to your
services.

We also removed the topicana classes in package `org.sdase.commons.server.kafka.topicana`.
For this reason the following methods were removed or changed
on [MessageListenerRegistration](./sda-commons-server-kafka/src/main/java/org/sdase/commons/server/kafka/builder/MessageListenerRegistration.java)
and [ProducerRegistration](./sda-commons-server-kafka/src/main/java/org/sdase/commons/server/kafka/builder/ProducerRegistration.java):

- `checkTopicConfiguration`
  
  This method compared the current topics with the configured ones and threw
  a `org.sdase.commons.server.kafka.topicana.MismatchedTopicConfigException` in case they did not
  match.
  It was removed in this version.

- `forTopicConfigs`
  
  This method accepted a list
  of `org.sdase.commons.server.kafka.topicana.ExpectedTopicConfiguration` to compare against the
  current topic configuration.
  Now it accepts a list of [TopicConfig](./sda-commons-server-kafka/src/main/java/org/sdase/commons/server/kafka/config/TopicConfig.java),
  with only the name of the topic, but it does not perform any check in the configuration.

- `forTopic(ExpectedTopicConfiguration topic)`
  
  This method accepted an instance
  of `org.sdase.commons.server.kafka.topicana.ExpectedTopicConfiguration` to compare against the
  current topic configuration.
  Now it accepts a [TopicConfig](./sda-commons-server-kafka/src/main/java/org/sdase/commons/server/kafka/config/TopicConfig.java)
  instance, with only the name of the topic, but it does not perform any check in the configuration.

### Kafka Configuration

As topic configuration is not defined in the service anymore, all properties but `name` have been
removed from the topic configuration.

```diff
# example changes in config.yml

kafka:
  topics:
    event-topic:
      name: ${KAFKA_CONSUMER_EVENT_TOPIC:-partner-ods-event-topic}
-     partitions: ${KAFKA_CONSUMER_EVENT_TOPIC_PARTITIONS:-2}
-     replicationFactor: ${KAFKA_CONSUMER_EVENT_TOPIC_REPLICATION_FACTOR:-1}

```

## 7. Deployment and Release of upgraded services

The changes mentioned above also have an impact on the deployment of a containerized service.
This section summarizes notable changes of deployments, that are derived from the required migration
in a service.

Services that upgrade to SDA Dropwizard Commons v3 should mention this in their release notes.
It may be required to introduce the upgrade as breaking change (aka new major release) if
deployments are incompatible to the previous version.

Additional service specific changes may apply as well.

### Tracing

Any Jaeger related configuration should be migrated to the Open Telemetry variant.
The release notes of a service should provide respective information.

Changes with backward compatible fallback:

- To identify the service, `JAEGER_SERVICE_NAME` is still supported.
  Changing to `OTEL_SERVICE_NAME` is recommended.
- To disable tracing, `JAEGER_SAMPLER_TYPE=const` in combination with `JAEGER_SAMPLER_PARAM=0` is
  still supported.
  Changing to `TRACING_DISABLED=true` is recommended.

**Incompatible changes:**

- `JAEGER_AGENT_HOST` is not supported anymore.
  [The official documentation](https://opentelemetry.io/docs/reference/specification/sdk-environment-variables/#otlp-exporter)
  provides all available options.
  In an environment that previously used Jaeger, `OTEL_TRACES_EXPORTER=jaeger` and
  `OTEL_EXPORTER_JAEGER_ENDPOINT=http://jaeger-collector.jaeger:14250` may be suitable.
  Note that the collector endpoint may not be the same as the agent endpoint configured for Jaeger.

### Kafka

All topic configurations for topic creation and validation are not considered anymore.
Users of the service should be informed that the service will not create any topic and does not
validate existing topics.
Therefore, some configuration options of a service may be removed.

### MongoDB

Configuration of individual parts of the MongoDB Connection String is deprecated.
Each service should provide a configuration option for the full MongoDB Connection String and
inform about the deprecation in the release notes.
