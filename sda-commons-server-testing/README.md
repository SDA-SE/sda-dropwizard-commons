# SDA Commons Server Testing

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-testing/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-testing)

The module `sda-commons-server-testing` is the base module to add unit and integrations test for applications in the 
SDA SE infrastructure.

It provides JUnit test rules that are helpful in integration tests.

It should be added with test scope and offers common test utilities with their dependencies in convergent versions that
match other SDA Commons modules. This way users can avoid to test their application with different versions the
application uses in production. Some modules of SDA Commons may have additional testing modules for specific support or
mocking.

For testing some frameworks are included:

| Group            | Artifact             | Version |
|------------------|----------------------|---------|
| `junit`          | `junit`              | 4.12    |
| `io.dropwizard`  | `dropwizard-testing` | 1.3.5   |
| `org.mockito`    | `mockito-core`       | 2.23.0  |
| `org.assertj`    | `assertj-core`       | 3.11.1  |

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

### LazyRule

The [`LazyRule`](./src/main/java/org/sdase/commons/server/testing/LazyRule.java) allows to wrap another 
rule to defer the initialization of the rule till the rule is started the first time. 
This allows to initialize a rule with parameters that are only available once another rule is 
completely initialized. This is often required if one rule opens a random port that the other rule 
want to connect to.

```
public class CustomIT {

    private static final ServerRule SERVER = new ServerRule();
    private static final LazyRule<ClientRule> CLIENT = new LazyRule<>(() -> new ClientRule(SERVER.getPort()));

    @ClassRule
    public static final RuleChain RULE_CHAIN = RuleChain.outerRule(SERVER).around(CLIENT);


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
