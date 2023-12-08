# SDA Commons Server S3 Testing

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-s3-testing/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-s3-testing)

This is accomplished using [local-s3](https://github.com/Robothy/local-s3), which
provides an in memory object storage.

## Usage

To use the test extension, a dependency to this module has to be added:

```
testCompile 'org.sdase.commons:sda-commons-server-s3-testing'
```

## JUnit 5

This module provides the [`S3ClassExtension`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-server-s3-testing/src/main/java/org/sdase/commons/server/s3/testing/S3ClassExtension.java),
a JUnit 5 extension that is used to automatically bootstrap an AWS S3-compatible object storage instance
for integration tests.

To create a S3 Mock instance, annotate your test class with `@LocalS3` 
and register the S3 test extension in your test class:

```java
@LocalS3
class MyTest {

    @RegisterExtension
    @Order(0)
    static final S3ClassExtension S3 = S3ClassExtension
            .builder()
            .build();
}
```

The extension takes care to choose a free port for the endpoint. You can access the
URL using `S3.getEndpoint()`.
Often one needs to pass the endpoint URL to the constructor of another extension:

```java
@LocalS3
class DropwizardIT {

  @RegisterExtension
  @Order(0)
  static final S3ClassExtension S3 = S3ClassExtension
        .builder()
        .build();

  @RegisterExtension
  @Order(1)
  static DropwizardAppExtension<TestConfiguration> DW =
    new DropwizardAppExtension<>(
          MyApplication.class,
          ResourceHelpers.resourceFilePath("test-config.yml"),
          ConfigOverride.config("objectstorage.endpoint", S3::getEndpoint));

}
```

In case you need a pre-populated bucket in your tests, you might add files while building the extension.
You can call `S3.resetAll()` to restore this state at any time. If you need to perform additional
operations on the object storage `S3.getClient()` provides a full S3 storage client.

```java
@RegisterExtension
@Order(0)
static final S3ClassExtension S3 = S3ClassExtension
     .builder()
     .createBucket("bucket-of-water")
     .putObject("bucket", "file.txt", new File(ResourceHelpers.resourceFilePath("test-file.txt")))
     .putObject("bucket", "stream.txt", MyClass.class.getResourceAsStream("/test-file.txt"))
     .putObject("bucket", "content.txt", "RUN SDA")
     .build();
```
