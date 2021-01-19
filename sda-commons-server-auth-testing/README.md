# SDA Commons Server Auth Testing

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-auth-testing/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-auth-testing)

This module provides support for testing applications that are secured with 
[`sda-commons-server-auth`](../sda-commons-server-auth/README.md). To use the support, this module has to be added as
dependency:

```
testCompile 'org.sdase.commons:sda-commons-server-auth-testing:<current-version>'
```

In an integration test, authentication can be configured using the 
[`AuthRule`](./src/main/java/org/sdase/commons/server/auth/testing/AuthRule.java) and/or the [`OpaRule`](./src/main/java/org/sdase/commons/server/opa/testing/OpaRule.java).

## Auth Rule
The `AuthRule` uses the `EnvironmentRule` to create the `AuthConfig` in an environment property called `AUTH_RULE`.
Therefore the configuration in the test needs to use this property and the application is required to use the 
[`ConfigurationSubstitutionBundle`](../sda-commons-server-dropwizard/src/main/java/org/sdase/commons/server/dropwizard/bundles/ConfigurationSubstitutionBundle.java)
from [`sda-commons-server-dropwizard`](../sda-commons-server-dropwizard/README.md):

```java
public class MyApp extends Application<MyConfig> {

   @Override
   public void initialize(Bootstrap<MyConfig> bootstrap) {
      bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
      bootstrap.addBundle(AuthBundle.builder().withAuthConfigProvider(MyConfig::getAuth).build());
   }

   @Override
   public void run(MyConfig configuration, Environment environment) {
      // ...
   }
}
```

```yaml
# test-config.yaml
server:
  applicationConnectors:
  - type: http
    port: 0
  adminConnectors:
  - type: http
    port: 0

# The configuration of the test auth bundle is injected here
auth: ${AUTH_RULE}
```

To implement the test, the `AuthRule` has to be applied around the `DropwizardAppRule`:

```java
public class AuthRuleIT {

   private static DropwizardAppRule<MyConfig> DW = new DropwizardAppRule<>(
         MyApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

   private static AuthRule AUTH = AuthRule.builder().build();

   @ClassRule
   public static RuleChain CHAIN = RuleChain.outerRule(AUTH).around(DW);

   // @Test
}
```

The `AuthRule` provides functions to generate a valid token that matches to the auth configuration in tests.
```java
   Response response = createWebTarget()
            .path("/secure")
            .request(APPLICATION_JSON)
            .headers(AUTH.auth()   
                  .addClaim("test", "testClaim")
                  .addClaims(singletonMap("mapKey", "testClaimFromMap"))
                  .buildAuthHeader())  // creates a valid Authorization header with a valid JWT 
            .get();
```

Examples can be found in the [test source branch](./src/test) of this module. There is

- [An example app](./src/test/java/org/sdase/commons/server/auth/testing/test/AuthTestApp.java)  
- [A test with authentication](./src/test/java/org/sdase/commons/server/auth/testing/AuthRuleIT.java)  
- [A test with disabled authentication](./src/test/java/org/sdase/commons/server/auth/testing/AuthDisabledIT.java)
- [An appropriate test `config.yaml`](./src/test/resources/test-config.yaml)

## Auth Extension
The `AuthExtension` puts the `AuthConfig` in an environment variable named `AUTH_RULE` (for backwards compatibility).
The configuration in the test needs to use this property and the application is required to use the
[`ConfigurationSubstitutionBundle`](../sda-commons-server-dropwizard/src/main/java/org/sdase/commons/server/dropwizard/bundles/ConfigurationSubstitutionBundle.java)
from [`sda-commons-server-dropwizard`](../sda-commons-server-dropwizard/README.md):

```java
public class MyApp extends Application<MyConfig> {

   @Override
   public void initialize(Bootstrap<MyConfig> bootstrap) {
      bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
      bootstrap.addBundle(AuthBundle.builder().withAuthConfigProvider(MyConfig::getAuth).build());
   }

   @Override
   public void run(MyConfig configuration, Environment environment) {
      // ...
   }
}
```

```yaml
# test-config.yaml
server:
  applicationConnectors:
  - type: http
    port: 0
  adminConnectors:
  - type: http
    port: 0

# The configuration of the test auth bundle is injected here
auth: ${AUTH_RULE}
```

To implement the test, the `AuthExtension` has to be initialized before the `DropwizardAppExtension`:

```java
class AuthExtensionIT {

  @Order(0)
  @RegisterExtension 
  static final AuthExtension AUTH = AuthExtension.builder().build();
  
  @Order(1)
  @RegisterExtension
  static final DropwizardAppExtension<AuthTestConfig> DW =
      new DropwizardAppExtension<>(
          AuthTestApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));
   // @Test
}
```

The `AuthExtension` provides functions to generate a valid token that matches to the auth configuration in tests.
```java
   Response response = createWebTarget()
            .path("/secure")
            .request(APPLICATION_JSON)
            .headers(AUTH.auth()   
                  .addClaim("test", "testClaim")
                  .addClaims(singletonMap("mapKey", "testClaimFromMap"))
                  .buildAuthHeader())  // creates a valid Authorization header with a valid JWT 
            .get();
```

Examples can be found in the [test source branch](./src/test) of this module. There is

- [An example app](./src/test/java/org/sdase/commons/server/auth/testing/test/AuthTestApp.java)
- [A test with authentication](./src/test/java/org/sdase/commons/server/auth/testing/AuthClassExtensionIT.java)
- [A test with disabled authentication](./src/test/java/org/sdase/commons/server/auth/testing/AuthDisabledJUnit5IT.java)
- [An appropriate test `config.yaml`](./src/test/resources/test-config.yaml)

## OPA Rule

The OPA Rule is built around WireMock. The mock can be configured via the rule.

To implement a test with an OPA Mock, the `OpaRule` has to be applied around the `DropwizardAppRule` with a RuleChain. Lazy Rule must be used since the OPA Mock starts on a random port. 

```java
public class OpaIT {

   private static final OpaRule OPA_RULE = new OpaRule();

   private static final DropwizardAppRule<OpaBundeTestAppConfiguration> DW =
         new DropwizardAppRule<>(OpaBundleTestApp.class, ResourceHelpers.resourceFilePath("test-opa-config.yaml"),
         config("opa.baseUrl", OPA_RULE::getUrl));


   @ClassRule
   public static RuleChain CHAIN = RuleChain.outerRule(OPA_RULE).around(DW);

   // @Test
}
```

To control the OPA mock behavior, the following API is provided
```java
 // allow access to a given httpMethod/path combination
 OPA_RULE.mock(onRequest().withHttpMethod(httpMethod).withPath(path).allow());
 // allow access to a given httpMethod/path/jwt combination
 OPA_RULE.mock(onRequest().withHttpMethod(httpMethod).withPath(path).withJwt(jwt).allow());
 // deny access to a given httpMethod/path combination
 OPA_RULE.mock(onRequest().withHttpMethod(httpMethod).withPath(path).deny());
 // allow access to a given httpMethod/path combination with constraint
 OPA_RULE.mock(onRequest().withHttpMethod(httpMethod).withPath(path).allow().withConstraint(new ConstraintModel(...)));
 // the response is returned for all requests, if no more specific mock is configured
 OPA_RULE.mock(onAnyRequest().answer(new OpaResponse(...)));
 
 // the same options are available for any requests if no more specific mock is configured
 OPA_RULE.mock(onAnyRequest().allow());
 OPA_RULE.mock(onAnyRequest().answer(new OpaResponse(...)));
 
 // It is possible to verify of the OPA has been invoked with parameters for the resource 
 // defined by the path and the httpMethod
 verify(int count, String httpMethod, String path)
 // it is also possible to check against a builder instance
 OPA_RULE.verify(1, onRequest().withHttpMethod(httpMethod).withPath(path).withJwt(jwt));
```

Examples can be found in the [test source branch](./src/test) of this module. There is

- [An example app](./src/test/java/org/sdase/commons/server/opa/testing/test/OpaBundleTestApp.java)  
- [A test with OPA](./src/test/java/org/sdase/commons/server/opa/testing/OpaIT.java)  
- [A test with disabled OPA support](./src/test/java/org/sdase/commons/server/opa/testing/OpaDisabledIT.java). In this case, only empty constraints are within the principal
- [An appropriate test `config.yaml`](./src/test/resources/test-opa-config.yaml)

Example with activated AUTH and OPA bundle can be found here:
- [Example app](./src/test/java/org/sdase/commons/server/opa/testing/test/AuthAndOpaBundleTestApp.java)
- [Test](./src/test/java/org/sdase/commons/server/opa/testing/AuthAndOpaIT.java)

## OPA Extension

The Junit 5 OPA Extension is built around WireMock. The mock can be configured via the extension.

To implement a test with an OPA Mock, the `OpaExtension` has to be initialized before `DropwizardAppExtension` implicitly by field declaration order or explicitly with a `@Order(N)`.

```java
import org.sdase.commons.server.testing.junit5.DropwizardAppExtension;

public class OpaIT {

  @Order(0)
  @RegisterExtension
  static final OpaExtension OPA_EXTENSION = new OpaExtension();

  @Order(1)
  @RegisterExtension
  static final DropwizardAppExtension<OpaBundeTestAppConfiguration> DW =
         new DropwizardAppExtension<>(
                  OpaBundleTestApp.class,
                  ResourceHelpers.resourceFilePath("test-opa-config.yaml"),
                  ConfigOverride.config("opa.baseUrl", OPA_EXTENSION::getUrl));

   // @Test
}
```

To control the OPA mock behavior, the following API is provided
```java
 // allow access to a given httpMethod/path combination
 OPA_EXTENSION.mock(onRequest().withHttpMethod(httpMethod).withPath(path).allow());
 // allow access to a given httpMethod/path/jwt combination
 OPA_EXTENSION.mock(onRequest().withHttpMethod(httpMethod).withPath(path).withJwt(jwt).allow());
 // deny access to a given httpMethod/path combination
 OPA_EXTENSION.mock(onRequest().withHttpMethod(httpMethod).withPath(path).deny());
 // allow access to a given httpMethod/path combination with constraint
 OPA_EXTENSION.mock(onRequest().withHttpMethod(httpMethod).withPath(path).allow().withConstraint(new ConstraintModel(...)));
 // the response is returned for all requests, if no more specific mock is configured
 OPA_EXTENSION.mock(onAnyRequest().answer(new OpaResponse(...)));
 
 // the same options are available for any requests if no more specific mock is configured
 OPA_EXTENSION.mock(onAnyRequest().allow());
 OPA_EXTENSION.mock(onAnyRequest().answer(new OpaResponse(...)));
 
 // It is possible to verify of the OPA has been invoked with parameters for the resource 
 // defined by the path and the httpMethod
 verify(int count, String httpMethod, String path)
 // it is also possible to check against a builder instance
 OPA_EXTENSION.verify(1, onRequest().withHttpMethod(httpMethod).withPath(path).withJwt(jwt));
```

Examples can be found in the [test source branch](./src/test) of this module. There is

- [An example app](./src/test/java/org/sdase/commons/server/opa/testing/test/OpaBundleTestApp.java)
- [A test with OPA](./src/test/java/org/sdase/commons/server/opa/testing/OpaClassExtensionIT.java)
- [A test with disabled OPA support](./src/test/java/org/sdase/commons/server/opa/testing/OpaDisabledJUnit5IT.java). In this case, only empty constraints are within the principal
- [An appropriate test `config.yaml`](./src/test/resources/test-opa-config.yaml)

Example with activated AUTH and OPA bundle can be found here:
- [Example app](./src/test/java/org/sdase/commons/server/opa/testing/test/AuthAndOpaBundleTestApp.java)
- [Test](./src/test/java/org/sdase/commons/server/opa/testing/AuthAndOpaClassExtensionIT.java)
