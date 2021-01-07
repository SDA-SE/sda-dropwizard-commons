# SDA Commons Server S3 Testing

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-s3-testing/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-s3-testing)

## Extension
This module provides the [`S3Extension`](src/main/java/org/sdase/commons/server/s3/testing/S3Extension.java),
a JUnit 5 extension that is used to automatically bootstrap an AWS S3-compatible object storage instance
for integration tests.

This is accomplished using [s3mock](https://github.com/findify/s3mock), which
provides an in memory object storage.

## Usage

To use the Junit 5 extension, a dependency to this module has to be added:

```
testCompile 'org.sdase.commons:sda-commons-server-s3-testing:<current-version>'
```

To create a S3 Mock instance, add the S3 test rule to your test class:

```
@RegisterExtension
static final S3Extension S3_EXTENSION = S3Extension
      .builder()
      .build();
```

The extension takes care to choose a free port for the endpoint. You can access the mock
URL using `S3_EXTENSION.getEndpoint()`.
Often one need to pass the endpoint URL to the constructor of another rule:

```

class DropwizardIT {

  @RegisterExtension
  static final S3Extension S3_EXTENSION = S3Extension
        .builder()
        .build();

  @RegisterExtension
  static DropwizardAppExtension<TestConfiguration> DW =
    new DropwizardAppExtension<>(
          MyApplication.class,
          ResourceHelpers.resourceFilePath("test-config.yml"),
          ConfigOverride.config("objectstorage.endpoint", () -> S3_EXTENSION.getEndpoint())));

}
```

In case you need a pre-populated bucket in your tests, you might add files while building the extension.
You can call `S3_EXTENSION.resetAll()` to restore this state at any time. If you need to perform additional
operations on the object storage `S3_EXTENSION.getClient()` provides a full S3 storage client.

```
@RegisterExtension
static final S3Extension S3_EXTENSION = S3Extension
     .builder()
     .createBucket("bucket-of-water")
     .putObject("bucket", "file.txt", new File(ResourceHelpers.resourceFilePath("test-file.txt")))
     .putObject("bucket", "stream.txt", MyClass.class.getResourceAsStream("/test-file.txt"))
     .putObject("bucket", "content.txt", "RUN SDA")
     .build();
```
