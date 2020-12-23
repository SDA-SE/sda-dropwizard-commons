package org.sdase.commons.server.auth.testing;

import static java.util.Collections.singletonList;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Configuration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.sdase.commons.server.auth.config.AuthConfig;
import org.sdase.commons.server.testing.Environment;

public class AuthExtension extends AbstractAuth implements BeforeAllCallback, AfterAllCallback {

  @SuppressWarnings("WeakerAccess")
  public static final String AUTH_RULE_ENV_KEY = "AUTH_RULE";

  private static final String DEFAULT_KEY_ID = AuthExtension.class.getSimpleName();
  private static final String DEFAULT_ISSUER = "AuthExtension";
  private static final String DEFAULT_SUBJECT = "test";
  private static final String DEFAULT_INTERNAL_KEY_PATH =
      "/org/sdase/commons/server/auth/testing"; // NOSONAR classpath Url is intentionally hardcoded
  private static final String DEFAULT_PRIVATE_KEY_LOCATION =
      AuthExtension.class.getResource(DEFAULT_INTERNAL_KEY_PATH + "/rsa-private.key").toString();
  private static final String DEFAULT_CERTIFICATE_LOCATION =
      AuthExtension.class.getResource(DEFAULT_INTERNAL_KEY_PATH + "/rsa-x.509.pem").toString();

  private String valueToRestore;

  /**
   * @return a builder that guides along required fields to fluently create a new {@link
   *     AuthExtension}
   */
  @SuppressWarnings("WeakerAccess")
  public static AuthExtension.AuthExtensionBuilder builder() {
    return new AuthExtension.Builder();
  }

  private AuthExtension(
      boolean disableAuth,
      String keyId,
      String issuer,
      String subject,
      String certificateLocation,
      String privateKeyLocation) {
    super(disableAuth, keyId, issuer, subject, privateKeyLocation, certificateLocation);

    if (disableAuth) {
      initDisabledTestAuth();
    } else {
      initEnabledTestAuth();
    }
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
  public void beforeAll(ExtensionContext context) {
    valueToRestore = System.getenv(AUTH_RULE_ENV_KEY);
  }

  @Override
  public void afterAll(ExtensionContext context) {
    Environment.setEnv(AUTH_RULE_ENV_KEY, valueToRestore);
  }

  /**
   * Provides a consumer that applies the {@link AuthConfig} matching this {@code AuthExtension} to
   * an application configuration in tests that do not use a configuration yaml file.
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

  private void initDisabledTestAuth() {
    this.authConfig = new AuthConfig().setDisableAuth(true);
    Environment.setEnv(AUTH_RULE_ENV_KEY, "{\"disableAuth\": true}");
  }

  private void initEnabledTestAuth() {
    this.privateKey = loadPrivateKey(this.privateKeyLocation);
    this.authConfig = new AuthConfig().setKeys(singletonList(createKeyLocation()));

    try {
      String authKeysConfig = new ObjectMapper().writeValueAsString(authConfig);
      Environment.setEnv(AUTH_RULE_ENV_KEY, authKeysConfig);
    } catch (JsonProcessingException e) {
      fail("Failed to create the config keys: " + e.getMessage());
    }
  }

  //
  // Builder
  //

  public interface AuthExtensionBuilder {
    AuthExtension.AuthExtensionBuilder withKeyId(String keyId);

    AuthExtension.AuthExtensionBuilder withIssuer(String issuer);

    AuthExtension.AuthExtensionBuilder withSubject(String subject);

    AuthExtension.AuthExtensionBuilder withCustomKeyPair(
        String publicKeyCertificateLocation, String privateKeyLocation);

    AuthExtension.DisabledBuilder withDisabledAuth();

    AuthExtension build();
  }

  public interface DisabledBuilder {
    AuthExtension build();
  }

  public static class Builder
      implements AuthExtension.AuthExtensionBuilder, AuthExtension.DisabledBuilder {

    private boolean disableAuth;
    private String keyId = DEFAULT_KEY_ID;
    private String issuer = DEFAULT_ISSUER;
    private String subject = DEFAULT_SUBJECT;
    private String publicKeyCertificateLocation = DEFAULT_CERTIFICATE_LOCATION;
    private String privateKeyLocation = DEFAULT_PRIVATE_KEY_LOCATION;

    private Builder() {}

    @Override
    public AuthExtension.AuthExtensionBuilder withKeyId(String keyId) {
      this.keyId = keyId;
      return this;
    }

    @Override
    public AuthExtension.AuthExtensionBuilder withIssuer(String issuer) {
      this.issuer = issuer;
      return this;
    }

    @Override
    public AuthExtension.AuthExtensionBuilder withSubject(String subject) {
      this.subject = subject;
      return this;
    }

    @Override
    public AuthExtension.AuthExtensionBuilder withCustomKeyPair(
        String publicKeyCertificateLocation, String privateKeyLocation) {
      this.publicKeyCertificateLocation = publicKeyCertificateLocation;
      this.privateKeyLocation = privateKeyLocation;
      return this;
    }

    @Override
    public AuthExtension.DisabledBuilder withDisabledAuth() {
      this.disableAuth = true;
      return this;
    }

    @Override
    public AuthExtension build() {
      return new AuthExtension(
          disableAuth, keyId, issuer, subject, publicKeyCertificateLocation, privateKeyLocation);
    }
  }
}
