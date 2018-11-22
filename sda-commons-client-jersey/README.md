# SDA Commons Client Jersey

The module `sda-commons-client-jersey` provides support for using Jersey clients within the dropwizard application.


## Usage

The [`JerseyClientBundle`](./src/main/java/org/sdase/commons/client/jersey/JerseyClientBundle.java) must be added to the 
application. It provides a [`ClientFactory`](./src/main/java/org/sdase/commons/client/jersey/ClientFactory.java) to 
create clients. The `ClientFactory` needs to be initialized and is available in the `run(...)` phase. Therefore the 
bundle should be declared as field and not in the `initialize` method.

```java
public class MyApplication extends Application<MyConfiguration> {
   private JerseyClientBundle jerseyClientBundle = JerseyClientBundle.builder().build();

   public static void main(final String[] args) {
      new MyApplication().run(args);
   }

   @Override
   public void initialize(Bootstrap<MyConfiguration> bootstrap) {
      // ...
      bootstrap.addBundle(jerseyClientBundle);
      // ...
   }

   @Override
   public void run(MyConfiguration configuration, Environment environment) {
      // ...
      ClientFactory clientFactory = jerseyClientBundle.getClientFactory();
   }
}
```

The `ClientFactory` is able to create Jersey clients from interfaces defining the API with JAX-RS annotations. It may 
also create generic Jersey clients that can build the request definition with a fluent api:

```java
Client googleClient = clientFactory.externalClient()
      .buildGenericClient("google")
      .target("https://maps.google.com");
Response response = googleClient.path("api")/* ... */.get();
```


## Configuration

All clients defined with the `ClientFactory` can be built either for the SDA Platform or for external services. Clients
for the SDA Platform have some magic added that clients for an external service don't have:

- _Trace-Token_

  SDA Platform clients always add the Trace-Token of the current incoming request context to the headers of the outgoing
  request. If no incoming request context is found, a new unique Trace-Token is generated for each request. It will be
  discarded when the outgoing request completes.
  
- _Authorization_

  SDA Platform clients can be configured to pass through the Authorization header of an incoming request context:
  
  ```java
  clientFactory.platformClient()
        .enableAuthenticationPassThrough()
        .api(OtherSdaServiceClient.class)
        .atTarget("http://other-sda-service.sda.net/api");
  ```
- _Consumer-Token_
  
  SDA Platform clients are able to send a Consumer-Token header to identify the caller. The token that is used has to be
  configured and published to the bundle. Currently the token is only the name of the consumer.
  
  ```java
  private JerseyClientBundle jerseyClientBundle = JerseyClientBundle.builder()
        .withConsumerTokenProvider(MyConfiguration::getConsumerToken).build();
  ```
  
  Then the clients can be configured to add a Consumer-Token header:
  
  ```java
  clientFactory.platformClient()
        .enableConsumerToken()
        .api(OtherSdaServiceClient.class)
        .atTarget("http://other-sda-service.sda.net/api");
  ```


## Writing API Clients as interfaces

Client interfaces use the same annotations as the service definitions for REST endpoints. An example is the 
[`MockApiClient`](./src/integTest/java/org/sdase/commons/client/jersey/test/MockApiClient.java) in the integration tests
of this module.

Error handling is different based on the return type:

If a specific return type is defined (e.g. `List<MyResource> getMyResources();`), it is only returned for successful 
requests in any error or redirect case, an exception is thrown. The thrown exception is a subclass of 
`javax.ws.rs.WebApplicationException`. 
- `javax.ws.rs.RedirectionException` to indicate redirects 
- `javax.ws.rs.ClientErrorException` for client errors
- `javax.ws.rs.ServerErrorException` for server errors

If a `javax.ws.rs.core.Response` is defined as return type, Http errors and redirects can be read from the `Response`
object. **Remember to always close the `Response` object. It references open socket streams.**

In both variants a `java.net.ConnectException` may be thrown if the client can't connect to the server.