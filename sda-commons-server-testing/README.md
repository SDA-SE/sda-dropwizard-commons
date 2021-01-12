# SDA Commons Server Testing

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-testing/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-testing)

The module `sda-commons-server-testing` is the base module to add unit and integrations test for applications in the 
SDA SE infrastructure.
It provides JUnit test rules and extensions that are helpful in integration tests. 

Add the module with test scope:

```groovy
  testCompile "org.sdase.commons:sda-commons-server-testing"
```
In case you want to use JUnit 5 you also have to activate it in your build.gradle:
```groovy
  test {
    useJUnitPlatform()
  }
```

## Provided Assertions

### [GoldenFileAssertions](./src/main/java/org/sdase/commons/server/testing/GoldenFileAssertions.java)

Special assertions for `Path` objects to check if a file matches the expected contents and updates
them if needed. These assertions are helpful to check if certain files are stored in the repository
(like [OpenAPI](../sda-commons-server-openapi) or [AsyncApi](../sda-commons-shared-asyncapi)).

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
content should always be reproducible. Note that the [AsyncAPI](../sda-commons-shared-asyncapi) and
[OpenAPI](../sda-commons-server-openapi) generations export reproducible content. 

## Provided Junit 5 Extensions

### EnvironmentExtension

The [`EnvironmentExtension`](./src/main/java/org/sdase/commons/server/testing/junit5/EnvironmentExtension.java) allows to override or
unset environment variables in test cases and resets them to the state before the test after the test finished.

```java
public class CustomIT {

    @RegisterExtension
    public final EnvironmentExtension ENV = new EnvironmentExtension()
            .setEnv("DISABLE_AUTH", Boolean.TRUE.toString())
            .setEnv("SERVICE_URL", () -> "http://localhost:8080")
            .unsetEnv("USER_NAME"); 

    // ...
}
```

## Provided Junit 4 Rules

### EnvironmentRule

The [`EnvironmentRule`](./src/main/java/org/sdase/commons/server/testing/EnvironmentRule.java) allows to override or
unset environment variables in test cases and resets them to the state before the test after the test finished.

```java
public class CustomIT {

    @ClassRule
    public static final EnvironmentRule ENV = new EnvironmentRule()
            .setEnv("DISABLE_AUTH", Boolean.TRUE.toString())
            .unsetEnv("USER_NAME");

    // ...
}
```

## Provided helpers

### DropwizardRuleHelper

The `DropwizardRuleHelper` allows to bootstrap a programmatically configured Dropwizard application in tests without the
need for `test-config.yaml`:

```java
public class CustomIT {

   @ClassRule
   public static DropwizardAppRule<TestConfig> DW = DropwizardRuleHelper.dropwizardTestAppFrom(TestApp.class)
         .withConfigFrom(TestConfig::new)
         .withRandomPorts()
         .withConfigurationModifier(c -> c.setMyConfigProperty("Foo"))
         .withConfigurationModifier(c -> c.setMyOtherConfigProperty("Bar"))
         .build();

    // ...
}
```

### DropwizardConfigurationHelper

The `DropwizardConfigurationHelper` allows to create a Dropwizard configuration programmatically in tests without the
need for `test-config.yaml`. This can be useful when not using the default DropwizardAppRule, e.g. in combination with
[`sda-commons-server-weld-testing`](../sda-commons-server-weld-testing/README.md):

```java
public class CustomIT {

   @ClassRule
   public static final DropwizardAppRule<AppConfiguration> RULE = new WeldAppRule<>(
         WeldExampleApplication.class,
         configFrom(AppConfiguration::new).withPorts(4567, 0).withRootPath("/api/*").build());

    // ...
}
```
