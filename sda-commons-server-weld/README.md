# SDA Commons Server Weld

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-weld/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-weld)

`sda-commons-server-weld` is used to bootstrap Dropwizard applications inside a Weld-SE container and provides CDI 
support for servlets, listeners and resources.
It allows to inject the application class or instances produced by the application class.

**Info:**
We at SDA SE do not use CDI in our microservices any more.
We believe that dependency injection is not helpful for small services.
Therefore this module is not actively maintained by SDA SE developers.
Automated security upgrades are enabled.
Contributions of new features and bug fixes are welcome.


## Usage

### Application Bootstrap

To bootstrap a Dropwizard application inside a Weld-SE container, use the [`DropwizardWeldHelper`](./src/main/java/org/sdase/commons/server/weld/DropwizardWeldHelper.java):

```java
public static void main(final String[]args) throws Exception {
    DropwizardWeldHelper.run(MyApplication.class, args);
}
```

### Provided Bundles

To optionally use CDI support inside of servlets, use the additional [`WeldBundle`](./src/main/java/org/sdase/commons/server/weld/WeldBundle.java):

```java
public void initialize(final Bootstrap<AppConfiguration> bootstrap) {
    bootstrap.addBundle(new WeldBundle());
}
```

### Testing

See [`sda-commons-server-weld-testing`](../sda-commons-server-weld-testing/README.md).
