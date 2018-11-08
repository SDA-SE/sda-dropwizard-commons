package org.sdase.commons.server.auth.testing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sdase.commons.server.auth.config.AuthConfig;
import org.sdase.commons.server.auth.config.KeyLocation;
import org.sdase.commons.server.auth.config.KeyUriType;
import org.sdase.commons.server.testing.EnvironmentRule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import static java.util.Collections.singletonList;
import static org.junit.Assert.fail;

/**
 * This {@link TestRule} configures a Dropwizard application that uses JWT authentication for integration tests. It
 * provides a local certificate for JWT verification and offers {@link AuthRule#auth() a builder} for creation of
 * tokens with optional custom claims. The rule may be used with custom issuer, subject, certificate and public key.
 * Issuer and subject may be customized for each token.
 */
public class AuthRule implements TestRule {

   public static final String AUTH_RULE_ENV_KEY = "AUTH_RULE";

   private static final String DEFAULT_KEY_ID = AuthRule.class.getSimpleName();
   private static final String DEFAULT_ISSUER = "AuthRule";
   private static final String DEFAULT_SUBJECT = "test";
   private static final String DEFAULT_INTERNAL_KEY_PATH = "/org/sdase/commons/server/auth/testing"; // NOSONAR classpath Url is intentionally hardcoded
   private static final String DEFAULT_PRIVATE_KEY_LOCATION =
         AuthRule.class.getResource(DEFAULT_INTERNAL_KEY_PATH + "/rsa-private.key").toString();
   private static final String DEFAULT_CERTIFICATE_LOCATION =
         AuthRule.class.getResource(DEFAULT_INTERNAL_KEY_PATH + "/rsa-x.509.pem").toString();

   private final boolean disableAuth;

   private final String keyId;

   private final String issuer;

   private final String subject;

   private RuleChain delegate;

   private RSAPrivateKey privateKey;

   private final String privateKeyLocation;

   private final String certificateLocation;

   /**
    * @return a builder that guides along required fields to fluently create a new {@link AuthRule}
    */
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
      this.disableAuth = disableAuth;
      this.keyId = keyId;
      this.issuer = issuer;
      this.subject = subject;
      this.privateKeyLocation = privateKeyLocation;
      this.certificateLocation = certificateLocation;
      init();
   }

   /**
    * @return a builder to configure JWT content and create a signed token that will be accepted by the application
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

   private void init() {
      if (disableAuth) {
         initDisabledTestAuth();
      }
      else {
         initEnabledTestAuth();
      }
   }

   private void initDisabledTestAuth() {
      delegate = RuleChain.outerRule(new EnvironmentRule().setEnv(AUTH_RULE_ENV_KEY, "{\"disableAuth\": true}"));
   }

   private void initEnabledTestAuth() {
      this.privateKey = loadPrivateKey(this.privateKeyLocation);
      KeyLocation keyLocation = new KeyLocation();
      keyLocation.setPemKeyId(keyId);
      keyLocation.setLocation(URI.create(certificateLocation));
      keyLocation.setType(KeyUriType.PEM);
      AuthConfig authConfig = new AuthConfig();
      authConfig.setKeys(singletonList(keyLocation));

      try {
         String authKeysConfig = new ObjectMapper().writeValueAsString(authConfig);
         delegate = RuleChain.outerRule(new EnvironmentRule().setEnv(AUTH_RULE_ENV_KEY, authKeysConfig));
      } catch (JsonProcessingException e) {
         fail("Failed to create the config keys: " + e.getMessage());
      }
   }

   private RSAPrivateKey loadPrivateKey(String privateKeyLocation) {
      try (InputStream is = URI.create(privateKeyLocation).toURL().openStream()) {
         byte[] privateKeyBytes = read(is);
         PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKeyBytes);
         KeyFactory keyFactory = KeyFactory.getInstance("RSA");
         return (RSAPrivateKey) keyFactory.generatePrivate(spec);
      } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
         return null;
      }
   }

   private byte[] read(InputStream is) throws IOException {
      try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
         int nRead;
         byte[] data = new byte[1024];
         while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
         }
         return buffer.toByteArray();
      }
   }


   //
   // Builder
   //

   public interface AuthRuleBuilder {
      AuthRuleBuilder withKeyId(String keyId);
      AuthRuleBuilder withIssuer(String issuer);
      AuthRuleBuilder withSubject(String subject);
      AuthRuleBuilder withCustomKeyPair(String publicKeyCertificateLocation, String privateKeyLocation);
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

      private Builder() {
      }

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
      public AuthRuleBuilder withCustomKeyPair(String publicKeyCertificateLocation, String privateKeyLocation) {
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
         return new AuthRule(disableAuth, keyId, issuer, subject, publicKeyCertificateLocation, privateKeyLocation);
      }
   }

}
