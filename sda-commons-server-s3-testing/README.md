# SDA Commons Server S3 Testing

This module provides the [`S3MockRule`](src/main/java/org/sdase/commons/server/s3/testing/S3MockRule.java), 
a JUnit test rule that is used to automatically bootstrap an AWS S3-compatible object storage instance
for integration tests.

This is accomplished using [s3mock](https://github.com/findify/s3mock), which
provides an in memory object storage.

## Usage

To use the test rule, a dependency to this module has to be added:

```
testCompile 'org.sdase.commons:sda-commons-server-s3-testing:<current-version>'
```

To create a S3 Mock instance, add the S3 test rule to your test class:

```
public static final S3MockRule S3_MOCK = S3MockRule
      .builder()
      .build();
```

The test rule takes care to choose a free port for the endpoint. You can access the mock 
URL using `RULE.getEndpoint()`.
Often one need to pass the endpoint URL to the constructor of another rule, where the 
[`LazyRule`](../sda-commons-server-testing/src/main/java/org/sdase/commons/server/testing/LazyRule.java) 
can be handy:

```
public static final S3MockRule S3_MOCK = S3MockRule
      .builder()
      .build();

private static final LazyRule<DropwizardAppRule<AppConfiguration>> DW =
    new LazyRule<>(
        () ->
            new DropwizardAppRule<>(
                MyApplication.class,
                ResourceHelpers.resourceFilePath("test-config.yml"),
                ConfigOverride.config("objectstorage.endpoint", S3_MOCK.getEndpoint())));

@ClassRule
public static final RuleChain CHAIN = RuleChain.outerRule(S3_MOCK).around(DW);
```

In case you need a pre-populated bucket in your tests, you might add files while building the rule.
You can call `S3_MOCK.resetAll()` to restore this state at any time. If you need to perform additional 
operations on the object storage `S3_MOCK.getClient()` provides a full S3 storage client.

```
@ClassRule
public static final S3MockRule S3_MOCK = S3MockRule
     .builder()
     .createBucket("bucket-of-water")
     .putObject("bucket", "file.txt", new File(ResourceHelpers.resourceFilePath("test-file.txt")))
     .putObject("bucket", "stream.txt", MyClass.class.getResourceAsStream("/test-file.txt"))
     .putObject("bucket", "content.txt", "RUN SDA")
     .build();
```
