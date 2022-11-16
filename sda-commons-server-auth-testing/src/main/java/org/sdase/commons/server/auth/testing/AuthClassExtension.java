package org.sdase.commons.server.auth.testing;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.sdase.commons.server.auth.config.AuthConfig;

public class AuthClassExtension extends AbstractAuth
    implements BeforeAllCallback, AfterAllCallback {

  private static final String AUTH_RULE_ENV_KEY = "AUTH_RULE";

  private static final String DEFAULT_KEY_ID = AuthClassExtension.class.getSimpleName();
  private static final String DEFAULT_ISSUER = "AuthExtension";
  private static final String DEFAULT_SUBJECT = "test";
  private static final String DEFAULT_INTERNAL_KEY_PATH =
      "/org/sdase/commons/server/auth/testing"; // NOSONAR classpath Url is intentionally hardcoded
  private static final String DEFAULT_PRIVATE_KEY_LOCATION =
      Objects.requireNonNull(
              AuthClassExtension.class.getResource(DEFAULT_INTERNAL_KEY_PATH + "/rsa-private.key"))
          .toString();
  private static final String DEFAULT_CERTIFICATE_LOCATION =
      Objects.requireNonNull(
              AuthClassExtension.class.getResource(DEFAULT_INTERNAL_KEY_PATH + "/rsa-x.509.pem"))
          .toString();

  private String valueToRestore;

  /**
   * @return a builder that guides along required fields to fluently create a new {@link
   *     AuthClassExtension}
   */
  @SuppressWarnings("WeakerAccess")
  public static AuthClassExtension.AuthExtensionBuilder builder() {
    return new AuthClassExtension.Builder();
  }

  private AuthClassExtension(
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
    valueToRestore = getCurrentValueForAuthRuleEnv();
  }

  @Override
  public void afterAll(ExtensionContext context) {
    setValueForAuthRuleEnv(valueToRestore);
  }

  private void initDisabledTestAuth() {
    this.authConfig = new AuthConfig().setDisableAuth(true);
    setValueForAuthRuleEnv("{\"disableAuth\": true}");
  }

  private void initEnabledTestAuth() {
    this.privateKey = loadPrivateKey(this.privateKeyLocation);
    this.authConfig = new AuthConfig().setKeys(singletonList(createKeyLocation()));

    try {
      String authKeysConfig = new ObjectMapper().writeValueAsString(authConfig);
      setValueForAuthRuleEnv(authKeysConfig);
    } catch (JsonProcessingException e) {
      fail("Failed to create the config keys: " + e.getMessage());
    }
  }

  private String getCurrentValueForAuthRuleEnv() {
    return System.getProperty(AUTH_RULE_ENV_KEY);
  }

  private void setValueForAuthRuleEnv(String value) {
    System.setProperty(AUTH_RULE_ENV_KEY, value);
  }

  //
  // Builder
  //

  public interface AuthExtensionBuilder {
    AuthClassExtension.AuthExtensionBuilder withKeyId(String keyId);

    AuthClassExtension.AuthExtensionBuilder withIssuer(String issuer);

    AuthClassExtension.AuthExtensionBuilder withSubject(String subject);

    AuthClassExtension.AuthExtensionBuilder withCustomKeyPair(
        String publicKeyCertificateLocation, String privateKeyLocation);

    AuthClassExtension.DisabledBuilder withDisabledAuth();

    AuthClassExtension build();
  }

  public interface DisabledBuilder {
    AuthClassExtension build();
  }

  public static class Builder
      implements AuthClassExtension.AuthExtensionBuilder, AuthClassExtension.DisabledBuilder {

    private boolean disableAuth;
    private String keyId = DEFAULT_KEY_ID;
    private String issuer = DEFAULT_ISSUER;
    private String subject = DEFAULT_SUBJECT;
    private String publicKeyCertificateLocation = DEFAULT_CERTIFICATE_LOCATION;
    private String privateKeyLocation = DEFAULT_PRIVATE_KEY_LOCATION;

    private Builder() {}

    @Override
    public AuthClassExtension.AuthExtensionBuilder withKeyId(String keyId) {
      this.keyId = keyId;
      return this;
    }

    @Override
    public AuthClassExtension.AuthExtensionBuilder withIssuer(String issuer) {
      this.issuer = issuer;
      return this;
    }

    @Override
    public AuthClassExtension.AuthExtensionBuilder withSubject(String subject) {
      this.subject = subject;
      return this;
    }

    @Override
    public AuthClassExtension.AuthExtensionBuilder withCustomKeyPair(
        String publicKeyCertificateLocation, String privateKeyLocation) {
      this.publicKeyCertificateLocation = publicKeyCertificateLocation;
      this.privateKeyLocation = privateKeyLocation;
      return this;
    }

    @Override
    public AuthClassExtension.DisabledBuilder withDisabledAuth() {
      this.disableAuth = true;
      return this;
    }

    @Override
    public AuthClassExtension build() {
      return new AuthClassExtension(
          disableAuth, keyId, issuer, subject, publicKeyCertificateLocation, privateKeyLocation);
    }
  }
}
