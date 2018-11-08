# SDA Commons Server Testing

The module `sda-commons-server-testing` is the base module to add unit and integrations test for applications in the 
SDA SE infrastructure.

It provides JUnit test rules that are helpful in integration tests.

It should be added with test scope and offers common test utilities with their dependencies in convergent versions that
match other SDA Commons modules. This way users can avoid to test their application with different versions the
application uses in production. Some modules of SDA Commons may have additional testing modules for specific support or
mocking.

For testing some frameworks are included:

| Group            | Artifact           | Version |
|------------------|--------------------|---------|
| junit            | junit              | 4.12    |
| io.dropwizard    | dropwizard-testing | 1.3.5   |
| org.mockito      | mockito-core       | 2.23.0  |
| org.assertj      | assertj-core       | 3.11.1  |
| com.google.truth | truth              | 0.42    |


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

    // ..
}
``` 