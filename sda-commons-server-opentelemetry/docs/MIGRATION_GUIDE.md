# Migration Guide

## Starter Bundle
 If you do not use sda-commons-starter with [SdaPlatformBundle](../../sda-commons-starter/src/main/java/org/sdase/commons/starter/SdaPlatformBundle.java), you need to remove the Jaeger bundle and OpenTracing bundle and add the OpenTelemetry bundle.

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
            .withExcludedUrlsPattern(Pattern.compile(String.join("|", excludedTracingUrls)))
            .build());
```

## Replace OpenTracing dependencies to OpenTelemetry dependencies

Before:
```gradle
testImplementation 'io.opentracing:opentracing-mock'
```

After:
```gradle
testImplementation 'io.opentelemetry:opentelemetry-sdk-testing'
```

## Executors need to be instrumented to follow traces when parts of the request are handled asynchronously.

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

## Environment variables

To disable tracing, you must set the variable `TRACING_DISABLED` to `true`. The legacy Jaeger environment variables are still supported, but they will be removed in later versions.

Before:
```properties
JAEGER_SAMPLER_TYPE=const
JAEGER_SAMPLER_TYPE=0
```

After:
```properties
TRACING_DISABLED=true
```

## New environment variables
In order to configure Open Telemetry, you have to set some environment variables:

| Name                          | Default value                                 | Description                                                                      |
|-------------------------------|-----------------------------------------------|----------------------------------------------------------------------------------|
| OTEL_PROPAGATORS              | jaeger,b3,tracecontext,baggage                | Propagators to be used as a comma-separated list                                 |
| OTEL_SERVICE_NAME             | value from env `JAEGER_SERVICE_NAME` (if set) | The service name that will appear on tracing                                     |
| OTEL_EXPORTER_JAEGER_ENDPOINT | http://otel-collector-gateway.jaeger:4317     | Full URL of the Jaeger HTTP endpoint. The URL must point to the jaeger collector |

A full list of the configurable properties can be found in the [General SDK Configuration](https://opentelemetry.io/docs/reference/specification/sdk-environment-variables/).

## New API for instrumentation
If you use the OpenTracing API for manual instrumentation, you will have to replace it with the OpenTelemetry API. You can find the [API Definition](https://www.javadoc.io/static/io.opentelemetry/opentelemetry-api/1.0.1/io/opentelemetry/api/GlobalOpenTelemetry.html) to see all the possible methods.

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

## Test Setup

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
  