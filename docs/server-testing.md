# SDA Commons Server Testing

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-testing/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-testing)

The module `sda-commons-server-testing` is the base module to add unit and integrations test for applications in the 
SDA SE infrastructure.
It provides JUnit test extensions that are helpful in integration tests. 

Add the module with test scope:

```groovy
  testCompile "org.sdase.commons:sda-commons-server-testing"
```
In case you want to use JUnit 5 you also have to activate it in your build.gradle:
```groovy
  test {
    useJUnitPlatform()
  }

  // in case you use the integrationTest plugin:
  integrationTest {
    useJUnitPlatform()
  }
```

## Provided Assertions

### [GoldenFileAssertions](https://github.com/SDA-SE/sda-dropwizard-commons/tree/master/sda-commons-server-testing/https://github.com/SDA-SE/sda-dropwizard-commons/tree/master/sda-commons-server-testing/src/main/java/org/sdase/commons/server/testing/GoldenFileAssertions.java)

Special assertions for `Path` objects to check if a file matches the expected contents and updates
them if needed. These assertions are helpful to check if certain files are stored in the repository
(like [OpenAPI](./server-open-api.md) or [AsyncApi](./shared-asyncapi.md)).

> Use this assertion if you want to conveniently store the latest copy of a file in your repository,
> and let the CI fail if an update has not been committed.

```java
public class MyTestIT {
  @Test
  public void shouldHaveSameFileInRepository() throws IOException {
    // get expected content from a generator or rest endpoint
    String expected = ...; 
  
    // get a path to the file that should be checked
    Path filePath = Paths.get("my-file.yaml");
  
    // assert the file and update the file afterwards
    GoldenFileAssertions.assertThat(filePath)
        .hasContentAndUpdateGolden(expected);
  }
}
```

There is also a `assertThat(...).hasYamlContentAndUpdateGolden(...)` variant that interprets the content as
YAML or JSON and ignores the order of keys. If possible, prefer the other variant since the written
content should always be reproducible. Note that the [AsyncAPI](./shared-asyncapi.md) and
[OpenAPI](./server-open-api.md) generations export reproducible content.

### SystemPropertyClassExtension

The [`SystemPropertyClassExtension`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/master/sda-commons-server-testing/src/main/java/org/sdase/commons/server/testing/SystemPropertyClassExtension.java)
allows for overriding or unsetting system properties for (integration) tests and resets them to their original value when the tests have finished.

To use the extension, register it to your test class via the JUnit5 `@RegisterExtension`:

```java
@RegisterExtension
public SystemPropertyClassExtension PROP =
  new SystemPropertyClassExtension()
    .setProperty(PROP_TO_SET, VALUE)
    .setProperty(PROP_TO_SET_SUPPLIER, () -> VALUE)
    .unsetProperty(PROP_TO_UNSET);
```

## Provided helpers

### DropwizardConfigurationHelper

The `DropwizardConfigurationHelper` allows to create a Dropwizard configuration programmatically in tests without the
need for `test-config.yaml`. This can be useful when not using the default DropwizardAppRule, e.g. in combination with
[`sda-commons-server-weld-testing`](./server-weld-testing.md):

```java
public class CustomIT {

  @RegisterExtension
  static final WeldAppExtension<AppConfiguration> APP =
      new WeldAppExtension<>(
          WeldExampleApplication.class,
          configFrom(AppConfiguration::new).withPorts(4567, 0).withRootPath("/api/*").build());

    // ...
}
```


## Provided JUnit 5 Extensions

### SystemPropertyClassExtension

This module provides the [`SystemPropertyClassExtension`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/master/sda-commons-server-testing/src/main/java/org/sdase/commons/server/testing/SystemPropertyClassExtension.java),
a JUnit5 test extension to set and unset system properties before running an integration test.

To use the extension register it to your test class via the JUnit5 `@RegisterExtension`:

```java
@RegisterExtension
static final SystemPropertyClassExtension PROP = 
  new SystemPropertyClassExtension()
    .setProperty(PROP_TO_SET, VALUE)
    .setProperty(PROP_TO_SET_SUPPLIER, () -> VALUE)
    .unsetProperty(PROP_TO_UNSET);
```

Set and overwritten values will be reset to their original value once the test has finished.

### JUnit Pioneer

For JUnit 5 we will not supply a replacement for the `EnvironmentRule` and `DropwizardRuleHelper`. 

* The `EnvironmentRule` can be replaced with [JUnit Pioneer](https://junit-pioneer.org/docs/environment-variables/) 
  capabilities. **Anyway please read the warning above for overriding environment variables.** 
* Use `DropwizardAppExtension` directly as a replacement for the `DropwizardRuleHelper`.
