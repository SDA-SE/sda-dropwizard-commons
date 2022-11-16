package org.sdase.commons.server.auth.testing;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Configuration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.sdase.commons.server.auth.config.AuthConfig;
import org.sdase.commons.server.testing.SystemPropertyRule;

/**
 * This {@link TestRule} configures a Dropwizard application that uses JWT authentication for
 * integration tests. It provides a local certificate for JWT verification and offers {@link
 * AuthRule#auth() a builder} for creation of tokens with optional custom claims. The rule may be
 * used with custom issuer, subject, certificate and public key. Issuer and subject may be
 * customized for each token.
 */
public class AuthRule extends AbstractAuth implements TestRule {

  @SuppressWarnings("WeakerAccess")
  public static final String AUTH_RULE_ENV_KEY = "AUTH_RULE";

  private static final String DEFAULT_KEY_ID = AuthRule.class.getSimpleName();
  private static final String DEFAULT_ISSUER = "AuthRule";
  private static final String DEFAULT_SUBJECT = "test";
  private static final String DEFAULT_INTERNAL_KEY_PATH =
      "/org/sdase/commons/server/auth/testing"; // NOSONAR classpath Url is intentionally hardcoded
  private static final String DEFAULT_PRIVATE_KEY_LOCATION =
      AuthRule.class.getResource(DEFAULT_INTERNAL_KEY_PATH + "/rsa-private.key").toString();
  private static final String DEFAULT_CERTIFICATE_LOCATION =
      AuthRule.class.getResource(DEFAULT_INTERNAL_KEY_PATH + "/rsa-x.509.pem").toString();

  private RuleChain delegate;

  /**
   * @return a builder that guides along required fields to fluently create a new {@link AuthRule}
   */
  @SuppressWarnings("WeakerAccess")
  public static AuthRuleBuilder builder() {
    return new Builder();
  }

  private AuthRule(
      boolean disableAuth,
      String keyId,
      String issuer,
      String subject,
      String certificateLocation,
      String privateKeyLocation) {
    super(disableAuth, keyId, issuer, subject, privateKeyLocation, certificateLocation);
    init();
  }

  /**
   * @return a builder to configure JWT content and create a signed token that will be accepted by
   *     the application
   */
  public AuthBuilder auth() {
    if (disableAuth) {
      throw new IllegalStateException("Could not create token when auth is disabled.");
    }
    return new AuthBuilder(keyId, privateKey).withIssuer(issuer).withSubject(subject);
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return delegate.apply(base, description);
  }

  /**
   * Provides a consumer that applies the {@link AuthConfig} matching this {@code AuthRule} to an
   * application configuration in tests that do not use a configuration yaml file.
   *
   * @param authConfigSetter a reference to the setter of the {@link AuthConfig} in the {@link
   *     Configuration} the application uses, e.g. {@code MyAppConfig::setAuth}
   * @param <C> the type of {@link Configuration} the application uses
   * @return a consumer to be used with {@link
   *     org.sdase.commons.server.testing.DropwizardRuleHelper#withConfigurationModifier(Consumer)}
   *     or {@link
   *     org.sdase.commons.server.testing.DropwizardConfigurationHelper#withConfigurationModifier(Consumer)}
   * @deprecated see {@link org.sdase.commons.server.testing.DropwizardRuleHelper}
   */
  @SuppressWarnings("WeakerAccess")
  @Deprecated
  public <C extends Configuration> Consumer<C> applyConfig(
      BiConsumer<C, AuthConfig> authConfigSetter) {
    return c -> authConfigSetter.accept(c, this.authConfig);
  }

  private void init() {
    if (disableAuth) {
      initDisabledTestAuth();
    } else {
      initEnabledTestAuth();
    }
  }

  private void initDisabledTestAuth() {
    this.authConfig = new AuthConfig().setDisableAuth(true);
    initTestAuth("{\"disableAuth\": true}");
  }

  private void initEnabledTestAuth() {
    this.privateKey = loadPrivateKey(this.privateKeyLocation);
    this.authConfig = new AuthConfig().setKeys(singletonList(createKeyLocation()));

    try {
      final String authKeysConfig = new ObjectMapper().writeValueAsString(authConfig);
      initTestAuth(authKeysConfig);
    } catch (JsonProcessingException e) {
      fail("Failed to create the config keys: " + e.getMessage());
    }
  }

  private void initTestAuth(String authConfig) {
    final TestRule testRule = createTestRule(authConfig);
    delegate = RuleChain.outerRule(testRule);
  }

  static TestRule createTestRule(String authConfig) {
    return new SystemPropertyRule().setProperty(AUTH_RULE_ENV_KEY, authConfig);
  }

  //
  // Builder
  //

  public interface AuthRuleBuilder {

    AuthRuleBuilder withKeyId(String keyId);

    AuthRuleBuilder withIssuer(String issuer);

    AuthRuleBuilder withSubject(String subject);

    AuthRuleBuilder withCustomKeyPair(
        String publicKeyCertificateLocation, String privateKeyLocation);

    DisabledBuilder withDisabledAuth();

    AuthRule build();
  }

  public interface DisabledBuilder {

    AuthRule build();
  }

  public static class Builder implements AuthRuleBuilder, DisabledBuilder {

    private boolean disableAuth;
    private String keyId = DEFAULT_KEY_ID;
    private String issuer = DEFAULT_ISSUER;
    private String subject = DEFAULT_SUBJECT;
    private String publicKeyCertificateLocation = DEFAULT_CERTIFICATE_LOCATION;
    private String privateKeyLocation = DEFAULT_PRIVATE_KEY_LOCATION;

    private Builder() {}

    @Override
    public AuthRuleBuilder withKeyId(String keyId) {
      this.keyId = keyId;
      return this;
    }

    @Override
    public AuthRuleBuilder withIssuer(String issuer) {
      this.issuer = issuer;
      return this;
    }

    @Override
    public AuthRuleBuilder withSubject(String subject) {
      this.subject = subject;
      return this;
    }

    @Override
    public AuthRuleBuilder withCustomKeyPair(
        String publicKeyCertificateLocation, String privateKeyLocation) {
      this.publicKeyCertificateLocation = publicKeyCertificateLocation;
      this.privateKeyLocation = privateKeyLocation;
      return this;
    }

    @Override
    public DisabledBuilder withDisabledAuth() {
      this.disableAuth = true;
      return this;
    }

    @Override
    public AuthRule build() {
      return new AuthRule(
          disableAuth, keyId, issuer, subject, publicKeyCertificateLocation, privateKeyLocation);
    }
  }
}
