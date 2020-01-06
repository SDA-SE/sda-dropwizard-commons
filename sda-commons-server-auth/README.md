# SDA Commons Server Auth

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
- a authentication provider root url or
- an url providing a [`JSON Web Key Set`](https://tools.ietf.org/html/draft-ietf-jose-json-web-key-41#section-5)

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

In this case, the `AUTH_KEYS` variable should contain a Json array of 
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

## OPA Bundle

Details about the authorization with Open Policy Agent are documented within the authorization concept (see Confluence). 
In short, Open Policy Agent acts as policy decision point and is started as sidecar to the actual service.

![Overview](./docs/Overview.svg)

The OPA bundle requests the policy decision providing the following inputs
 * HTTP path as Array
 * HTTP method as String
 * validated JWT (if available) 
 * all request headers 
 
_Remark to HTTP request headers:_  
The bundle normalizes  header names to lower case to simplify handling in OPA since HTTP specification defines header names as case insensitive.
Multivalued headers are not normalized with respect to the representation as list or single string with separator char.
They are forwarded as parsed by the framework. 
  

These inputs can be accessed inside a policy `.rego`-file in this way:
```
# decode the JWT as new variable 'token'
token = {"payload": payload} {
    not input.jwt == null
    io.jwt.decode(input.jwt, [_, payload, _])
}

# deny by default
default allow = false

allow {
    # allow if path match '/contracts' 
    input.path = ["contracts"]

    # allow if request method 'GET' is used
    input.httpMethod == "GET"

    # allow if 'claim' exists in the JWT payload
    token.payload.claim

    # allow if a request header 'HttpRequestHeaderName' has a certain value 
    input.headers["httprequestheadername"][_] == "certain-value" 
}
```

The response consists of two parts, the overall `allow` decision and optional rules that represent constraints to limit data access
within the service. The constraints are fully service dependent and MUST be applied when querying the database or
filtering received data. 

The following listing presents a sample OPA result with a positive allow decision and two constraints, the first with boolean value and second
with a list of string values.
```
{
  "result": {
    "allow": true,
    "constraint1": true,
    "constraint2": [ "v2.1", "v2.2" ]
  }
}
```

The bundle creates a [`OpaJwtPrincipal`](./src/main/java/org/sdase/commons/server/opa/OpaJwtPrincipal.java) for each 
request. Data from an [`JwtPrincipal`](./src/main/java/org/sdase/commons/server/auth/JwtPrincipal.java) is copied to the new principal if existing.
Beside the JWT, the constraints are included in this principal. The `OpaJwtPrincipal` includes a method to parse the constraints JSON string to
a Java object.

The following listing shows a corresponding model class to the example above.
```java
public class ConstraintModel {
  
  private boolean constraint1;
  private List<String> constraint2;
  
}
```

The principal can be accessed from the `SecurityContext`.

```java
@Path("/secure")
public class SecureEndPoint {

   @Context
   private SecurityContext securityContext;

   @GET
   public Response getSomethingSecure() {
      OpaJwtPrincipal opaJwtPrincipal = (OpaJwtPrincipal) securityContext.getUserPrincipal();
      // ...
   }
}
```

To activate the OPA integration in an application, the bundle has to be added to the application. The kubernetes file of the 
service must also be adjusted to start OPA as sidecar.

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

The config includes the connection data to the opa sidecar and the path to the used policy endpoint.

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
  # readTimeout for OPA requests in millis, default 500
  readTimeout: 500
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

## Testing

[`sda-commons-server-auth-testing`](../sda-commons-server-auth-testing/README.md) provides support for testing
applications with authentication.
