# SDA Commons Server Testing

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-testing/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-testing)

The module `sda-commons-server-testing` is the base module to add unit and integrations test for applications in the 
SDA SE infrastructure.
It provides JUnit test rules that are helpful in integration tests. 

Add the module with test scope:

```groovy
  testCompile "org.sdase.commons:sda-commons-server-testing"
```

## Provided Rules

### EnvironmentRule

The [`EnvironmentRule`](./src/main/java/org/sdase/commons/server/testing/EnvironmentRule.java) allows to override or
unset environment variables in test cases and resets them to the state before the test after the test finished.

```
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
