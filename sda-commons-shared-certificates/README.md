# SDA Commons Shared Certificates

This module is responsible for looking CA certificates in `PEM` format in a 
default (but configurable) directory and putting the parsed certificates into the truststore.
These certificates are used to verify SSL connections to the database.

## Usage

The [`CaCertificatesBundle`](./src/main/java/org/sdase/commons/shared/certificates/ca/CaCertificatesBundle.java) 
should be added as a field in the bundle class instead of being anonymously added in the 
`initialize` method like other bundles of this library, so we can use it to get the SSLContext in the `run` method.

```java
public class MyBundle<C extends Configuration> implements ConfiguredBundle<C> {

  private final CaCertificatesBundle.FinalBuilder<C> caCertificatesBundleBuilder;
  private CaCertificatesBundle<C> caCertificatesBundle;
  private SSLContext sslContext;

  private MyBundle(
      //...
      CaCertificatesBundle.FinalBuilder<C> caCertificatesBundleBuilder
  ) {
    // ...
    this.caCertificatesBundleBuilder = caCertificatesBundleBuilder;
  }
  
   @Override
   public void initialize(Bootstrap<?> bootstrap) {
     this.caCertificatesBundle = caCertificatesBundleBuilder.build();
     bootstrap.addBundle((ConfiguredBundle) this.caCertificatesBundle);
   }

   // ...

  @Override
  public void run(C configuration, Environment environment) {
    //...
    // get the sslContext instance produced by the caCertificateBundle
    this.sslContext = this.caCertificatesBundle.getSslContext();
  }

  public static class Builder<T extends Configuration> {

    private CaCertificatesBundle.FinalBuilder<T> caCertificatesBundleBuilder =
        CaCertificatesBundle.builder();

    public Builder<T> withCaCertificateConfigProvider(
        CaCertificateConfigurationProvider<T> configProvider) {
      this.caCertificatesBundleBuilder =
          CaCertificatesBundle.builder().withCaCertificateConfigProvider(configProvider);
      return this;
    }

    //...

    public MyBundle<T> build(){
      return new MyBundle<>(
          //...,
          caCertificatesBundleBuilder
      );
    }
  }
  
}
```

## Configuration

The Dropwizard applications config class needs to provide a
[`CaCertificateConfiguration`](./src/main/java/org/sdase/commons/shared/certificates/ca/CaCertificateConfiguration.java).

The directory that contains CA certificates in PEM format is configured in the `config.yaml` 
of the final application config. 

Example config for **production** to be used with environment variables of the cluster configuration:

```yaml
caCertificate:
  customCaCertificateDir: "${CA_CERTIFICATE_CUSTOM_DIR:-/var/trust/certificates}"
```

If no configuration is used in a service, the default path `/var/trust/certificates` is checked for PEM certificates. 
This is the preferred approach to keep all services using this library consistent. 
But configuration may be needed if unit tests must cover the use of a custom SSLContext.