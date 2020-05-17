# SDA Commons Server Weld

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-weld/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-weld)

`sda-commons-server-weld` is used to bootstrap Dropwizard applications inside a Weld-SE container and provides CDI 
support for servlets, listeners and resources.

Compared to our old [`WeldBundle`](https://github.com/SDA-SE/rest-common), the new module is creating the Weld context 
outside of the application and therefore allows to inject the application class or instances produced by the application 
class. 

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
