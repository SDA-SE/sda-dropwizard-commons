# SDA Commons Server Auth

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-auth/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-auth)

This module provides an [`AuthBundle`](./src/main/java/org/sdase/commons/server/auth/AuthBundle.java) to authenticate
users based on JSON Web Tokens and an [`OpaBundle`](./src/main/java/org/sdase/commons/server/opa/OpaBundle.java) to
authorize the request with help of the [`Open Policy Agent`](http://www.openpolicyagent.org).

To use the bundle, a dependency to this module has to be added:

```
compile 'org.sdase.commons:sda-commons-server-auth:<current-version>'
```
## Auth Bundle

The authentication creates a [`JwtPrincipal`](./src/main/java/org/sdase/commons/server/auth/JwtPrincipal.java) per 
request. This can be accessed from the `SecurityContext`:

```java
@PermitAll
@Path("/secure")
public class SecureEndPoint {

   @Context
   private SecurityContext securityContext;

   @GET
   public Response getSomethingSecure() {
      JwtPrincipal jwtPrincipal = (JwtPrincipal) securityContext.getUserPrincipal();
      // ...
   }
}

```

To activate the authentication in an application, the bundle has to be added to the application:

```java
public class MyApplication extends Application<MyConfiguration> {
   
    public static void main(final String[] args) {
        new MyApplication().run(args);
    }

   @Override
   public void initialize(Bootstrap<MyConfiguration> bootstrap) {
      // ...
      bootstrap.addBundle(AuthBundle.builder().withAuthConfigProvider(MyConfiguration::getAuth).withAnnotatedAuthorization().build());
      // ...
   }

   @Override
   public void run(MyConfiguration configuration, Environment environment) {
      // ...
   }
}
```

The Bundle can be configured to perform basic authorization based on whether a token is presented or not on endpoints
that are annotated with `@PermitAll` with the setting `.withAnnotatedAuthorization()`.
If the authorization should be handled by e.g. the OpaBundle, the option `.withExternalAuthorization()` still validates
tokens sent in the `Authorization` header but also accepts requests _without header_ to be processed and eventually
rejected in a later stage.

## Configuration

The configuration relies on the `config.yaml` of the application and the custom property where the 
[`AuthConfig`](./src/main/java/org/sdase/commons/server/auth/config/AuthConfig.java) is mapped. Usually this should be 
`auth`.

```java
public class MyConfig extends Configuration {
   private AuthConfig auth;
   
   // getters and setters
}
```

The config allows to set the _leeway_ in seconds for validation of 
[`exp`](https://tools.ietf.org/html/rfc7519#section-4.1.4) and 
[`nbf`](https://tools.ietf.org/html/rfc7519#section-4.1.5).

Multiple sources for public keys for verification of the signature can be configured. Each source may refer to a 

- certificate in PEM format
- a authentication provider root URL or
- an URL providing a [`JSON Web Key Set`](https://tools.ietf.org/html/draft-ietf-jose-json-web-key-41#section-5)

The authentication can be disabled for use in test and development environments. **Be careful to NEVER disable
authentication in production.**

Example config:
```yaml
auth:
  # Disable all authentication, should be NEVER true in production
  disableAuth: false
  # The accepted leeway in seconds:
  leeway: 2
  # Definition of key sources providing public keys to verify signed tokens.
  keys:
    # A public key derived from a local PEM certificate.
    # The pemKeyId must match the 'kid' the signing authority sets in the token.
    # It may be null if the 'kid' in the token is not set by the signing authority.
    # The pemKeyId is only considered for type: PEM
  - type: PEM
    location: file:///secure/example.pem
    pemKeyId: example
    # A public key derived from a PEM certificate that is provided from a http server
    # The pemKeyId must match the 'kid' the signing authority sets in the token.
    # It may be null if the 'kid' in the token is not set by the signing authority.
    # The pemKeyId is only considered for type: PEM
  - type: PEM
    location: http://example.com/keys/example.pem
    pemKeyId: example.com
    # Public keys will be loaded from the OpenID provider using discovery.
  - type: OPEN_ID_DISCOVERY
    location: https://keycloak.example.com/auth/realms/my-realm
  - # Public keys will be loaded directly from the JWKS url of the OpenID provider.
    type: JWKS
    location: https://keycloak.example.com/auth/realms/my-realm/protocol/openid-connect/certs
```

The config may be filled from environment variables if the
[`ConfigurationSubstitutionBundle`](../sda-commons-server-dropwizard/src/main/java/org/sdase/commons/server/dropwizard/bundles/ConfigurationSubstitutionBundle.java)
is used:

```yaml
auth:
  disableAuth: ${DISABLE_AUTH:-false}
  leeway: ${AUTH_LEEWAY:-0}
  keys: ${AUTH_KEYS:-[]}
```

In this case, the `AUTH_KEYS` variable should contain a JSON array of 
[`KeyLocation`](./src/main/java/org/sdase/commons/server/auth/config/KeyLocation.java) objects:

```json
[
  {
    "type": "OPEN_ID_DISCOVERY", 
    "location": "https://keycloak.example.com/auth/realms/my-realm"
  },
  {
    "type": "OPEN_ID_DISCOVERY", 
    "location": "https://keycloak.example.com/auth/realms/my-other-realm"
  }
]
```

### HTTP Client Configuration and Proxy Support

The client that calls the OpenID Discovery endpoint or the JWKS url, is configurable with the standard
[Dropwizard configuration](https://www.dropwizard.io/en/latest/manual/configuration.html#man-configuration-clients-http).

> Tip: There is no need to make all configuration properties available as environment variables.
> Seldomly used properties can always be configured using [System Properties](https://www.dropwizard.io/en/latest/manual/core.html#man-core-configuration).

```yaml
auth:
  keyLoaderClient:
    timeout: 500ms
    proxy:
      host: 192.168.52.11
      port: 8080
      scheme : http
```

This configuration can be used to configure a proxy server if needed.
Use this if all clients should use an individual proxy configuration.

In addition, the client consumes the standard [proxy system properties](https://docs.oracle.com/javase/7/docs/api/java/net/doc-files/net-properties.html#Proxies).
Please note that a specific proxy configuration in the `HttpClientConfiguration` disables the proxy system properties for the client using that configuration.
This can be helpful when all clients in an Application should use the same proxy configuration (this includes all clients that are created by the [`sda-commons-client-jersey` bundle](../sda-commons-client-jersey).

## OPA Bundle

Details about the authorization with Open Policy Agent are documented within the authorization concept (see Confluence). 
In short, Open Policy Agent acts as policy decision point and is started as sidecar to the actual service.

![Overview](./docs/Overview.svg)

The OPA bundle requests the policy decision providing the following inputs
 * HTTP path as Array
 * HTTP method as String
 * validated JWT (if available) 
 * all request headers (can be disabled in the [`OpaBundle`](./src/main/java/org/sdase/commons/server/opa/OpaBundle.java) builder)

_Remark to HTTP request headers:_  
The bundle normalizes  header names to lower case to simplify handling in OPA since HTTP specification defines header names as case insensitive.
Multivalued headers are not normalized with respect to the representation as list or single string with separator char.
They are forwarded as parsed by the framework. 

_Security note:_
Please be aware while a service might only consider one value of a specific header, the OPA is able to authorize on a array of those.
Consider this in your policy when you want to make sure you authorize on the same value that a service might use to evaluate the output.

These [inputs](./src/main/java/org/sdase/commons/server/opa/filter/model/OpaInput.java) can be accessed inside a policy `.rego`-file in this way:
```rego
# each policy lies in a package that is referenced in the configuration of the OpaBundle
package example

# decode the JWT as new variable 'token'
token = {"payload": payload} {
    not input.jwt == null
    io.jwt.decode(input.jwt, [_, payload, _])
}

# deny by default
default allow = false

allow {
    # allow if path match '/contracts/:anyid' 
    input.path = ["contracts", _]

    # allow if request method 'GET' is used
    input.httpMethod == "GET"

    # allow if 'claim' exists in the JWT payload
    token.payload.claim

    # allow if a request header 'HttpRequestHeaderName' has a certain value 
    input.headers["httprequestheadername"][_] == "certain-value"
}

# set some example constraints 
constraint1 := true                # always true
constraint2 := [ "v2.1", "v2.2" ]  # always an array of "v2.1" and "v2.2"
constraint3[token.payload.sub].    # always a set that contains the 'sub' claim from the token
                                   # or is empty if no token is present

```

The response consists of two parts: The overall `allow` decision, and optional rules that represent _constraints_ to limit data access
within the service. These constraints are fully service dependent and MUST be applied when querying the database or
filtering received data.

The following listing presents a sample OPA result with a positive allow decision and two constraints, the first with boolean value and second
with a list of string values.
```json
{
  "result": {
    "allow": true,
    "constraint1": true,
    "constraint2": [ "v2.1", "v2.2" ],
    "constraint3": ["my-sub"]
  }
}
```

The following listing shows a corresponding model class to the example above:
```java
public class ConstraintModel {

  private boolean constraint1;

  private List<String> constraint2;
  
  // could also be a Set<String>
  private List<String> constraint3;

}
```

The bundle creates a [`OpaJwtPrincipal`](./src/main/java/org/sdase/commons/server/opa/OpaJwtPrincipal.java) 
for each request. You can retrieve the constraint model's data from the principal by invoking `OpaJwtPrincipal#getConstraintsAsEntity`.
Data from an [`JwtPrincipal`](./src/main/java/org/sdase/commons/server/auth/JwtPrincipal.java) is 
copied to the new principal if existing.
Beside the JWT, the constraints are included in this principal. The `OpaJwtPrincipal` includes a 
method to parse the constraints JSON string to a Java object.
The [`OpaJwtPrincipal`](./src/main/java/org/sdase/commons/server/opa/OpaJwtPrincipal.java) can be 
injected as field using `@Context` in 
[request scoped beans like endpoint implementations](../sda-commons-server-auth-testing/src/test/java/org/sdase/commons/server/opa/testing/test/OpaJwtPrincipalEndpoint.java) or accessed from the `SecurityContext`.

```java
@Path("/secure")
public class SecureEndPoint {

   @Context
   private OpaJwtPrincipal opaJwtPrincipal;

   @GET
   public Response getSomethingSecure() {
      // ...
   }
}
```

To activate the OPA integration in an application, the bundle has to be added to the application. The Kubernetes file of the 
service must also be adjusted to start OPA as sidecar.

> If you use the SdaPlatformBundle, there is a [more convenient way to enable OPA support](../sda-commons-starter).

```java
public class MyApplication extends Application<MyConfiguration> {
   
    public static void main(final String[] args) {
        new MyApplication().run(args);
    }

   @Override
   public void initialize(Bootstrap<MyConfiguration> bootstrap) {
      // ...
      bootstrap.addBundle(OpaBundle.builder().withOpaConfigProvider(OpaBundeTestAppConfiguration::getOpaConfig).build());
      // ...
   }

   @Override
   public void run(MyConfiguration configuration, Environment environment) {
      // ...
   }
}
```

## Configuration
The configuration relies on the `config.yaml` of the application and the custom property where the 
[`OpaConfig`](./src/main/java/org/sdase/commons/server/opa/config/OpaConfig.java) is mapped. Usually this should be 
`opa`.

```java
public class MyConfig extends Configuration {
   private OpaConfig opa;
   
   // getters and setters
}
```

The config includes the connection data to the OPA sidecar and the path to the used policy endpoint.

Example config:
```yaml
opa:
  # Disable authorization. An empty prinicpal is created with an empty set of constraints
  disableOpa: false
  # Url to the OPA sidecar
  baseUrl: http://localhost:8181
  # Package name of the policy file that should be evaluated for authorization decision
  # The package name is used to resolve the full path
  policyPackage: http.authz
  # Advanced configuration of the HTTP client that is used to call the Open Policy Agent
  opaClient:
    # timeout for OPA requests, default 500ms
    timeout: 500ms
```

The config may be filled from environment variables if the
[`ConfigurationSubstitutionBundle`](../sda-commons-server-dropwizard/src/main/java/org/sdase/commons/server/dropwizard/bundles/ConfigurationSubstitutionBundle.java)
is used:

```yaml
opa:
  disableOpa: ${DISABLE_OPA:-false}
  baseUrl: ${OPA_URL:-http://localhost:8181}
  policyPackage: ${OPA_POLICY_PACKAGE}
```

### HTTP Client Configuration and Proxy Support

The client that calls the Open Policy Agent is configurable with the standard
[Dropwizard configuration](https://www.dropwizard.io/en/latest/manual/configuration.html#man-configuration-clients-http).

> Tip: There is no need to make all configuration properties available as environment variables.
> Seldomly used properties can always be configured using [System Properties](https://www.dropwizard.io/en/latest/manual/core.html#man-core-configuration).
> 
```yaml
opa:
  opaClient:
    timeout: 500ms
```

This configuration can be used to configure a proxy server if needed.

*Please note that this client __does not__ consume the standard proxy system properties but needs to be configured manually! 
The OPA should be deployed as near to the service as possible, so we don't expect the need for universal proxy settings.*

## Testing

[`sda-commons-server-auth-testing`](../sda-commons-server-auth-testing/README.md) provides support for testing
applications with authentication.

## Input Extensions

The Bundle offers the option to register extensions that send custom data to the Open Policy Agent to be accessed during policy execution.
Such extensions implement the [`OpaInputExtension`](.src/main/java/org/sdase/commons/server/opa/extension/OpaInputExtension.java) interface and are registered in a dedicated namespace.
The extension is called in the `OpaAuthFilter` and is able to access the current `RequestContext` to for example extract additional data from the request.
_Overriding existing input properties (`path`, `jwt`, `httpMethod`) is not possible._

This extension option should only be used if the normal authorization via constraints is not powerful enough.
In general, a custom extension should not be necessary for most use cases.

_Remark on accessing the request body in an input extension:_
The extension gets the `ContainerRequestContext` as input to access information about the request.
Please be aware to not access the request entity since this might break your service.
A test that shows the erroneous behavior can be found in [`OpaBundleBodyInputExtensionTest.java`](./src/test/java/org/sdase/commons/server/opa/OpaBundleBodyInputExtensionTest.java)

_Security note:_
When creating new extensions, be aware that you might access properties of a request that are not yet validated in the method interface.
This is especially important if your service expects single values that might be accessible as array value to the OPA.
While the service (e.g. in case of query parameters) only considers the first value, make sure to not authorize on other values in the OPA.  

The following listing shows an example extension that adds a fixed boolean entry:
```java
public class ExampleOpaInputExtension implements OpaInputExtension<Boolean> {
  @Override
  public Boolean createAdditionalInputContent(ContainerRequestContext requestContext) {
    return true;
  }
}
```

Register the extension during the `OpaBundle` creation:
```java
OpaBundle.builder()
    .withOpaConfigProvider(YourConfiguration::getOpa)
    .withInputExtension("myExtension", new ExampleOpaInputExtension())
    .build();
```

Access the additional input inside a policy `.rego`-file in this way:
```rego
exampleExtensionWorks {
    # check if path match '/contracts' 
    input.path = ["contracts"]

    # check if the custom input has a certain value 
    input.myExtension == true 
}
```
