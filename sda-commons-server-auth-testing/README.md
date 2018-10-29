# SDA Commons Server Auth Testing

This module provides support for testing applications that are secured with 
[`sda-commons-server-auth`](../sda-commons-server-auth/README.md). To use the support, this module has to be added as
dependency:

```
testCompile 'org.sdase.commons:sda-commons-server-auth-testing:<current-version>'
```

In an integration test, authentication can be configured using the 
[`AuthRule`](./src/main/java/com/sdase/commons/server/auth/testing/AuthRule.java).

The `AuthRule` uses the `EnvironmentRule` to create the `AuthConfig` in an environment property called `AUTH_RULE`.
Therefore the configuration in the test needs to use this property and the application is required to use the 
[`ConfigurationSubstitutionBundle`](../sda-commons-server-dropwizard/src/main/java/com/sdase/commons/server/dropwizard/bundles/ConfigurationSubstitutionBundle.java)
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

Examples can be found in the [integTest source branch](./src/integTest) of this module. There is

- [An example app](./src/integTest/java/com/sdase/commons/server/auth/testing/test/AuthTestApp.java)  
- [A test with authentication](./src/integTest/java/com/sdase/commons/server/auth/testing/AuthRuleIT.java)  
- [A test with disabled authentication](./src/integTest/java/com/sdase/commons/server/auth/testing/AuthDisabledIT.java)
  which uses the `EnvironmentRule` instead of the `AuthRule`
- [An appropriate test config yaml](./src/integTest/resources/test-config.yaml)