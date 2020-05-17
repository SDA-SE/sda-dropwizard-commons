# SDA Commons Shared Forms

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-shared-forms/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-shared-forms)

The module `sda-commons-shared-forms` adds all required dependencies to support `multipart/*` in Dropwizard
applications.

Clients will automatically configured to support `multipart/*` if this module is available.

To support `multipart/*` as a server, Dropwizard's `io.dropwizard.forms.MultiPartBundle` has to be added:

```java
public class MyApplication extends Application<MyConfiguration> {

   public static void main(final String[] args) {
      new MyApplication().run(args);
   }

   @Override
   public void initialize(Bootstrap<MyConfiguration> bootstrap) {
      // ...
      bootstrap.addBundle(new io.dropwizard.forms.MultiPartBundle());
      // ...
   }

   @Override
   public void run(MyConfiguration configuration, Environment environment) {
      // ...
   }
}

```