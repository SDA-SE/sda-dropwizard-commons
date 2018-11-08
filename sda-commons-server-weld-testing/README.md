# SDA Commons Server Weld Testing

`sda-commons-server-weld-testing` is used to bootstrap Dropwizard applications inside a Weld-SE container using the
`DropwizardAppRule` during testing and provides CDI support for servlets, listeners and resources.

## Usage

### Testing

To start a Dropwizard application during testing the [`WeldAppRule`](./src/main/java/org/sdase/commons/server/weld/testing/WeldAppRule.java) can be used:

```java
@ClassRule
public static final WeldAppRule<AppConfiguration> RULE = new WeldAppRule<>(
    Application.class, ResourceHelpers.resourceFilePath("config.yml"));
```
 
The `WeldAppRule` is a shortcut for creating a `DropwizardAppRule` in combination with the [`WeldTestSupport`](./src/main/java/org/sdase/commons/server/weld/testing/WeldTestSupport.java):
 
```java
@ClassRule
public static final DropwizardAppRule<AppConfiguration> RULE = new DropwizardAppRule<>(
    new WeldTestSupport<>(MyApplication.class, ResourceHelpers.resourceFilePath("config.yml")));
```
