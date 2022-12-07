# SDA Commons Server Weld Testing

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-weld-testing/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-weld-testing)

`sda-commons-server-weld-testing` is used to bootstrap Dropwizard applications inside a Weld-SE container using the
`DropwizardAppExtension` during testing and provides CDI support for Servlets, listeners and resources.

**Info:**
We at SDA SE do not use CDI in our microservices anymore.
We believe that dependency injection is not helpful for small services.
Therefore, this module is not actively maintained by SDA SE developers.
Automated security upgrades are enabled.
Contributions of new features and bug fixes are welcome.

## Usage

### Testing

To start a Dropwizard application during testing the [`WeldAppExtension`](./src/main/java/org/sdase/commons/server/weld/testing/WeldAppExtension.java) can be used:

```java
public class WeldAppITest {

  @RegisterExtension
  static final WeldAppExtension<AppConfiguration> APP =
      new WeldAppExtension<>(Application.class, ResourceHelpers.resourceFilePath("config.yml"));

    // ...
} 
```
 
The `WeldAppExtension` is a shortcut for creating a `DropwizardAppExtension` in combination with the
[`WeldTestSupport`](./src/main/java/org/sdase/commons/server/weld/testing/WeldTestSupport.java).

It may also be used with programmatic configuration omitting a `config.yaml`:

```java
public class WeldAppITest {

  @RegisterExtension
  static final DropwizardAppExtension<AppConfiguration> APP = new WeldAppExtension<>(
      MyApplication.class,
      configFrom(AppConfiguration::new).withPorts(4567, 0).withRootPath("/api/*").build());

  // ...
}
```
