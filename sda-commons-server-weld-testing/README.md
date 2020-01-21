# SDA Commons Server Weld Testing

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-weld-testing/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-weld-testing)

`sda-commons-server-weld-testing` is used to bootstrap Dropwizard applications inside a Weld-SE container using the
`DropwizardAppRule` during testing and provides CDI support for Servlets, listeners and resources.

## Usage

### Testing

To start a Dropwizard application during testing the [`WeldAppRule`](./src/main/java/org/sdase/commons/server/weld/testing/WeldAppRule.java) can be used:

```java
public class WeldAppIT {

   @ClassRule
   public static final WeldAppRule<AppConfiguration> RULE = new WeldAppRule<>(
         Application.class, ResourceHelpers.resourceFilePath("config.yml"));

    // ...
} 
```
 
The `WeldAppRule` is a shortcut for creating a `DropwizardAppRule` in combination with the [`WeldTestSupport`](./src/main/java/org/sdase/commons/server/weld/testing/WeldTestSupport.java):
 
```java
public class WeldAppIT {

   @ClassRule
   public static final DropwizardAppRule<AppConfiguration> RULE = new DropwizardAppRule<>(
         new WeldTestSupport<>(MyApplication.class, ResourceHelpers.resourceFilePath("config.yml")));

    // ...
} 
```

It may also be used with programmatic configuration omitting a `config.yaml`:

```java
public class WeldAppIT {

   @ClassRule
   public static final DropwizardAppRule<AppConfiguration> RULE = new WeldAppRule<>(
         WeldExampleApplication.class,
         configFrom(AppConfiguration::new).withPorts(4567, 0).withRootPath("/api/*").build());

    // ...
}
```
