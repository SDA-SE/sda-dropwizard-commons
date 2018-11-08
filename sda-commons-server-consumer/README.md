# SDA Commons Server Consumer
  
The module `sda-commons-server-consumer` adds support to track or require a consumer token identifying the calling 
application.

Consumers can be differentiated in logs and metrics via a consumers token. The consumer token is provided via the 
`Consumer-Token` header. Right now a consumer token is only a name, no verification is done.

The consumer name will be derived from the received token (currently they are the same) and is added to the 
[`MDC`](https://www.slf4j.org/manual.html#mdc) and as request property.

If the consumer token is configured as required (= _not optional_) which is the default when using the yaml 
configuration, the server will respond `401 Unauthorized` when the client does not provide a consumer token.

## Usage

The consumer token is loaded within a filter that is created and registered by the 
[`ConsumerTokenBundle`](./src/main/java/org/sdase/commons/server/consumer/ConsumerTokenBundle.java) which must be added
to the Dropwizard application:

```java
public class MyApplication extends Application<MyConfiguration> {
   
    public static void main(final String[] args) {
        new MyApplication().run(args);
    }

   @Override
   public void initialize(Bootstrap<MyConfiguration> bootstrap) {
      // ...
      bootstrap.addBundle(ConsumerTokenBundle.builder()
            .withConfigProvider(MyConfiguration::getConsumerToken) // required or optional is configurable in config.yml
            // .withRequiredConsumerToken() // alternative: always require the token
            // .withOptionalConsumerToken() // alternative: never require the token but track it if available
            .build());
      // ...
   }

   @Override
   public void run(MyConfiguration configuration, Environment environment) {
      // ...
   }
}
```

## Configuration

When the bundle is initialized `withConfigProvider`, the configuration class needs a field for the 
`ConsumerTokenConfig`:

```java
public class MyConfig extends Configuration {

   private ConsumerTokenConfig consumerToken = new ConsumerTokenConfig();

   public ConsumerTokenConfig getConsumerToken() {
      return consumerToken;
   }

   public void setConsumerToken(ConsumerTokenConfig consumerToken) {
      this.consumerToken = consumerToken;
   }
}
```

In the `config.yml` the consumer token may be configured with support for environment properties when the
[`ConfigurationSubstitutionBundle`](../sda-commons-server-dropwizard/src/main/java/org/sdase/commons/server/dropwizard/bundles/ConfigurationSubstitutionBundle.java)
is used:

```yaml
consumerToken:
  optional: ${CONSUMER_TOKEN_OPTIONAL:-false}
```

