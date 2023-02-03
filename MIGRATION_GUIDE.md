# Migration Guide from v2 to v3

The following modules contain changes:

1. [sda-commons-server-testing](#1-sda-commons-server-testing)
2. [sda-commons-server-auth-testing](#2-sda-commons-server-auth-testing)
3. [sda-commons-server-opentracing](#3-sda-commons-server-opentracing)
4. [sda-commons-server-morphia](#4-sda-commons-server-morphia)
5. [sda-commons-server-kafka](#5-sda-commons-server-kafka)

## 1 sda-commons-server-testing

Does not provide any Junit 4 rules anymore. You should find Junit 5 extensions for all of your
rules.
We recommend to migrate all your Junit 4 tests to Junit 5.

## 2 sda-commons-server-auth-testing

Please change your `test-config.yaml` if they use `${AUTH_RULE}` as placeholder.
We wanted to get rid of all references to old Junit 4 rules.

Before:
```yaml
  config: ${AUTH_RULE}
```

After:
```yaml
  config: ${AUTH_CONFIG_KEYS}
```

## 3 sda-commons-server-opentracing

Migrate from OpenTracing to OpenTelemetry.

### Starter Bundle

If you do not use sda-commons-starter with [SdaPlatformBundle](./sda-commons-starter/src/main/java/org/sdase/commons/starter/SdaPlatformBundle.java),
you need to remove the Jaeger bundle and OpenTracing bundle and add the OpenTelemetry bundle.

Before:
```java
bootstrap.addBundle(JaegerBundle.builder().build());
bootstrap.addBundle(OpenTracingBundle.builder().build());
```

After:
```java
bootstrap.addBundle(
    OpenTelemetryBundle.builder()
      .withAutoConfiguredTelemetryInstance()
      .withExcludedUrlsPattern(Pattern.compile(String.join("|", Arrays.asList(
        "/ping", "/healthcheck", "/healthcheck/internal", "/metrics",
        "/metrics/prometheus"))))
      .build());
```

### Replace OpenTracing dependencies to OpenTelemetry dependencies

Before:
```gradle
testImplementation 'io.opentracing:opentracing-mock'
```

After:
```gradle
testImplementation 'io.opentelemetry:opentelemetry-sdk-testing'
```

### Executors need to be instrumented to follow traces when parts of the request are handled asynchronously.

Before:
```java
return new AsyncEmailSendManager(
  new InMemoryAsyncTaskRepository<>(limits.getMaxAttempts()),
  environment
      .lifecycle()
      .executorService("email-sender-%d")
      .minThreads(1)
      .maxThreads(limits.getMaxParallel())
      .build(),
```

After:
```java
return new AsyncEmailSendManager(
    new InMemoryAsyncTaskRepository<>(limits.getMaxAttempts()),
    io.opentelemetry.context.Context.taskWrapping(
      environment
          .lifecycle()
          .executorService("email-sender-%d")
          .minThreads(1)
          .maxThreads(limits.getMaxParallel())
          .build()),
```

### Environment variables

To disable tracing, you must set the variable `TRACING_DISABLED` to `true`.
The legacy Jaeger environment variables are still supported, but they will be removed in later
versions.

Before:
```properties
JAEGER_SAMPLER_TYPE=const
JAEGER_SAMPLER_TYPE=0
```

After:
```properties
TRACING_DISABLED=true
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

Before:
```java
public void sendEmail(To recipient, String subject, EmailBody body) {
    Span span = GlobalTracer.get().buildSpan("sendSmtpMail").start();
    try (Scope ignored = GlobalTracer.get().scopeManager().activate(span)) {
      delegate.sendEmail(recipient, subject, body);
    } finally {
      span.finish();
    }
  }
```

After:
```java
@Override
  public void sendEmail(To recipient, String subject, EmailBody body) {
    var tracer = GlobalOpenTelemetry.get().getTracer(getClass().getName());
    var span = tracer.spanBuilder("sendSmtpMail").startSpan();
    try (var ignored = span.makeCurrent()) {
      delegate.sendEmail(recipient, subject, body);
    } finally {
      span.end();
    }
  }
```

### Test Setup

- Add the opentelemetry test dependency
  ```gradle
    testImplementation 'io.opentelemetry:opentelemetry-sdk-testing' 
  ```

- Replace `io.opentracing.mock.MockTracer` with `io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension`

  Before:
    ```java
      private final MockTracer mockTracer = new MockTracer();
    ```

  After:
    ```java
      @RegisterExtension
      @Order(0)
      static final OpenTelemetryExtension OTEL = OpenTelemetryExtension.create();
    ```

- Clear metrics and spans before each test using OpenTelemetryExtension

  Before:

  ```java
    @BeforeEach
    void setupTestData() throws FolderException {
      GlobalTracerTestUtil.setGlobalTracerUnconditionally(mockTracer);
      mockTracer.reset();
    }
  ``` 
  After:
  ```java
  @BeforeEach
  void setupTestData() throws FolderException {
    OTEL.clearMetrics(); // this is the OpenTelemetryExtension instance
    OTEL.clearSpans();
  }
  ```

- Capture spans using `OpenTelemetryExtension` and `io.opentelemetry.sdk.trace.data.SpanData`

  Before:
  ```java
  assertThat(mockTracer.finishedSpans().stream()
                          .filter(s -> s.operationName().equals("expectedTracing")));
  ```
  After:
  ```java
  assertThat(OTEL.getSpans().stream().filter(s -> s.getName().equals("expectedTracing")));
  ```


## 4 sda-commons-server-morphia

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

### Morphia compatibility

When upgrading a service from 2.x.x a set of converters is provided to stay compatible with a
database that was initialized with Morphia.
To enable these converters, `.withMorphiaCompatibility()` can be used when building the
SpringDataMongoBundle.

```java
SpringDataMongoBundle.builder()
  .withConfigurationProvider(AppConfiguration::getSpringDataMongo)
  .withMorphiaCompatibility()
  .build();
```

### Validation

Automatic JSR validation is no longer provided in v3.
If you still want to validate your models you can do so manually using the `ValidationFactory`:

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
public void save(SampleEntity entity) {
    mongoOperations.insert(entity);
```

Find by id:
```java
public Optional<SampleEntity> findById(String id) {
    return Optional.ofNullable(mongoOperations.findById(id, SampleEntity.class));
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

Now we can define queries directly through the method names (see the official documentation for the specific syntax).
```java
public interface PersonMongoRepository extends MongoRepository<Person, String> {
  Optional<Person> findByName(String name);

  List<Person> findAllByAgeIsLessThanEqual(int age);

  List<Person> findAllByNameIsNot(String name);
}
```

See the [official documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.details)
for further details.

### Query functions

As Spring Data Mongo doesn't support/provide many query functions provided by Morphia, below are
some replacements/alternatives.

* #### _EqualIgnoreCase_
  Morphia supports usage of _equalIgnoreCase()_. Use _regex()_ in Spring Data Mongo. For example
  * Morphia - `query.criteria("fieldName").equalIgnoreCase(entity.getFieldName());`
  * Spring Data Mongo - `query.addCriteria(where("fieldName").regex(Pattern.compile("^" + Pattern.quote(entity.getFieldName()), Pattern.CASE_INSENSITIVE)));`

* #### _HasAnyOf_
  Morphia supports _hasAnyOf()_ method. Use _in()_ in Spring Data Mongo. For example
  * Morphia - `query.field("id").hasAnyOf(getIds());`
  * Spring Data Mongo - `query.addCriteria(where("id").in(getIds()));`

* #### _Filter_
  Morphia supports _filter()_ method. Use _is()_ in Spring Data Mongo. For example
  * Morphia - `query.filter("id", getId());`
  * Spring Data Mongo - `query.addCriteria(where("id").is(getId()));`

* #### _Contains_
  Morphia supports _contains()_ method. Use _regex()_ in Spring Data Mongo. For example
  * Morphia - `query.criteria("fieldName").contains(entity.getFieldName());`
  * Spring Data Mongo - `query.addCriteria(where("fieldName").regex(Pattern.compile(".*" + Pattern.quote(entity.getFieldName()) + ".*")));`

* #### _HasAllOf_
  Morphia supports _contains()_ method. Use _regex()_ in Spring Data Mongo. For example
  * Morphia - `query.field("fieldName").hasAllOf(getFieldNameValues());`
  * Spring Data Mongo - `query.addCriteria(where("fieldName").all(getFieldNameValues()));`


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


## 5 sda-commons-server-kafka

Some deprecated code was removed.
Especially we removed the feature `createTopicIfMissing` because we don't recommend services to do
that.
You usually need different privileges for topic creation that you don't want to give to your
services.