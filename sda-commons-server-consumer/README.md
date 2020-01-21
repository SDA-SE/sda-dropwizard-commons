# SDA Commons Server Consumer

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-consumer/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-consumer)

The module `sda-commons-server-consumer` adds support to track or require a consumer token identifying the calling 
application.

Consumers can be differentiated in logs and metrics via a consumers token. The consumer token is provided via the 
`Consumer-Token` header. Right now a consumer token is only a name, no verification is done.

The consumer name will be derived from the received token (currently they are the same) and is added to the 
[`MDC`](https://www.slf4j.org/manual.html#mdc) and as request property.

If the consumer token is configured as required (= _not optional_) which is the default when using the YAML 
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
            .withConfigProvider(MyConfiguration::getConsumerToken) // required with exclude or optional is configurable in config.yml
            // alternative1: always require the token if path in not matched by exclude pattern
            // .withRequiredConsumerToken().withExcludePattern("publicResource/\\d+.*")                                
            // alternative2: never require the token but track it if available
            // .withOptionalConsumerToken() 
            .build());
      // ...
   }

   @Override
   public void run(MyConfiguration configuration, Environment environment) {
      // ...
   }
}
```

### Exclude patterns
When consumer token is set to required, exclude regex patterns can be defined to exclude some URLs to require a consumer token. The regex must match
the resource path to exclude it.
 
E.g. in `http://localhost:8080/api/projects/1`, `http://localhost:8080/api/` is the base path
and `projects/1` is the resource path. 
 
#### swagger.json and swagger.yaml
When the `SwaggerBundle` from [`sda-commons-server-swagger`](../sda-commons-server-swagger/README.md) is in the 
classpath, excludes for _swagger.json_ and _swagger.yaml_ are added automatically using the regular expression 
`swagger\.(json|yaml)` to allow clients to load the swagger definition without providing a consumer token.

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
  excludePatterns: ${CONSUMER_TOKEN_EXCLUDE:-[]}
```

