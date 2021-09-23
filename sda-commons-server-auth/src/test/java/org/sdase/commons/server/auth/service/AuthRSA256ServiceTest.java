package org.sdase.commons.server.auth.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import io.dropwizard.testing.ResourceHelpers;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.auth.error.JwtAuthException;
import org.sdase.commons.server.auth.key.RsaPublicKeyLoader;
import org.sdase.commons.server.auth.service.testsources.JwksTestKeySource;

/**
 * This will validate the AuthRSA256Service by this unit test with the inclusion of the token issuer
 */
class AuthRSA256ServiceTest {

  public static final String CLAIM_ISSUER = "iss";
  public static final String CLAIM_NOT_BEFORE = "nbf";
  public static final String CLAIM_EXPIRE = "exp";
  public static final String RSA_PRIVATE_KEY = "rsa-private.key";
  private AuthRSA256Service service;
  private RsaPublicKeyLoader keyLoader;
  public static final String ISSUER = "https://localhost.com/issuer";
  public static final String KEY_ID = "myKeyId";

  @BeforeEach
  void setUp() {
    this.keyLoader = new RsaPublicKeyLoader();
    this.service = new AuthRSA256Service(this.keyLoader, 0);
  }

  @Test
  void validTokenWithKeyIdAndNoIssuerAndRequiredIssuerButJwks() {
    final Pair<RSAPrivateKey, RSAPublicKey> keyPair = createKeyPair(RSA_PRIVATE_KEY);
    String token = createToken(keyPair, ISSUER, KEY_ID, 0, 30);
    keyLoader.addKeySource(new JwksTestKeySource(null, keyPair.getRight(), ISSUER, KEY_ID));

    final Map<String, Claim> claims = this.service.auth(token);

    assertThat(claims.get(CLAIM_ISSUER).asString()).isEqualTo(ISSUER);
    assertThat(claims.get(CLAIM_NOT_BEFORE).asLong() * 1000L).isLessThan(new Date().getTime());
    assertThat(claims.get(CLAIM_EXPIRE).asLong() * 1000L).isGreaterThan(new Date().getTime());
  }

  @Test
  void validTokenWithoutIssuerAndRequiredIssuerButJwks() {
    final Pair<RSAPrivateKey, RSAPublicKey> keyPair = createKeyPair(RSA_PRIVATE_KEY);
    String token = createToken(keyPair, ISSUER, null, 0, 30);
    keyLoader.addKeySource(new JwksTestKeySource(null, keyPair.getRight(), ISSUER, null));

    final Map<String, Claim> claims = this.service.auth(token);

    assertThat(claims.get(CLAIM_ISSUER).asString()).isEqualTo(ISSUER);
    assertThat(claims.get(CLAIM_NOT_BEFORE).asLong() * 1000L).isLessThan(new Date().getTime());
    assertThat(claims.get(CLAIM_EXPIRE).asLong() * 1000L).isGreaterThan(new Date().getTime());
  }

  @Test
  void validTokenWithKeyIdAndNoIssuerButConfiguredRequiredIssuer() {
    final Pair<RSAPrivateKey, RSAPublicKey> keyPair = createKeyPair(RSA_PRIVATE_KEY);
    String token = createToken(keyPair, null, KEY_ID, 0, 30);
    keyLoader.addKeySource(new JwksTestKeySource(ISSUER, keyPair.getRight(), null, KEY_ID));

    final Map<String, Claim> claims = this.service.auth(token);

    assertThat(claims.get(CLAIM_ISSUER)).isNull();
    assertThat(claims.get(CLAIM_NOT_BEFORE).asLong() * 1000L).isLessThan(new Date().getTime());
    assertThat(claims.get(CLAIM_EXPIRE).asLong() * 1000L).isGreaterThan(new Date().getTime());
  }

  @Test
  void validTokenWithoutIssuerButConfiguredRequiredIssuer() {
    final Pair<RSAPrivateKey, RSAPublicKey> keyPair = createKeyPair(RSA_PRIVATE_KEY);
    String token = createToken(keyPair, null, null, 0, 30);
    keyLoader.addKeySource(new JwksTestKeySource(ISSUER, keyPair.getRight(), null, null));

    final Map<String, Claim> claims = this.service.auth(token);

    assertThat(claims.get(CLAIM_ISSUER)).isNull();
    assertThat(claims.get(CLAIM_NOT_BEFORE).asLong() * 1000L).isLessThan(new Date().getTime());
    assertThat(claims.get(CLAIM_EXPIRE).asLong() * 1000L).isGreaterThan(new Date().getTime());
  }

  @Test
  void validTokenWithKeyIdAndIssuerAndNoConfiguredRequiredIssuer() {
    final Pair<RSAPrivateKey, RSAPublicKey> keyPair = createKeyPair(RSA_PRIVATE_KEY);
    String token = createToken(keyPair, ISSUER, KEY_ID, 0, 30);
    keyLoader.addKeySource(new JwksTestKeySource(ISSUER, keyPair.getRight(), null, KEY_ID));

    final Map<String, Claim> claims = this.service.auth(token);

    assertThat(claims.get(CLAIM_ISSUER).asString()).isEqualTo(ISSUER);
    assertThat(claims.get(CLAIM_NOT_BEFORE).asLong() * 1000L).isLessThan(new Date().getTime());
    assertThat(claims.get(CLAIM_EXPIRE).asLong() * 1000L).isGreaterThan(new Date().getTime());
  }

  @Test
  void validTokenWithIssuerAndNoConfiguredRequiredIssuer() {
    final Pair<RSAPrivateKey, RSAPublicKey> keyPair = createKeyPair(RSA_PRIVATE_KEY);
    String token = createToken(keyPair, ISSUER, null, 0, 30);
    keyLoader.addKeySource(new JwksTestKeySource(ISSUER, keyPair.getRight(), null, null));

    final Map<String, Claim> claims = this.service.auth(token);

    assertThat(claims.get(CLAIM_ISSUER).asString()).isEqualTo(ISSUER);
    assertThat(claims.get(CLAIM_NOT_BEFORE).asLong() * 1000L).isLessThan(new Date().getTime());
    assertThat(claims.get(CLAIM_EXPIRE).asLong() * 1000L).isGreaterThan(new Date().getTime());
  }

  @Test
  void validTokenWithKeyIdAndIssuerAndAndFutureNotBeforeFailed() throws InterruptedException {
    final Pair<RSAPrivateKey, RSAPublicKey> keyPair = createKeyPair(RSA_PRIVATE_KEY);
    String token = createToken(keyPair, ISSUER, KEY_ID, 2, 30);
    keyLoader.addKeySource(new JwksTestKeySource(ISSUER, keyPair.getRight(), ISSUER, KEY_ID));

    assertThatThrownBy(() -> this.service.auth(token)).isInstanceOf(JwtAuthException.class);
    TimeUnit.SECONDS.sleep(2);
    final Map<String, Claim> claims = this.service.auth(token);

    assertThat(claims.get(CLAIM_ISSUER).asString()).isEqualTo(ISSUER);
    assertThat(claims.get(CLAIM_NOT_BEFORE).asLong() * 1000L).isLessThan(new Date().getTime());
    assertThat(claims.get(CLAIM_EXPIRE).asLong() * 1000L).isGreaterThan(new Date().getTime());
  }

  @Test
  void validTokenWithIssuerAndAndFutureNotBeforeFailed() throws InterruptedException {
    final Pair<RSAPrivateKey, RSAPublicKey> keyPair = createKeyPair(RSA_PRIVATE_KEY);
    String token = createToken(keyPair, ISSUER, null, 2, 30);
    keyLoader.addKeySource(new JwksTestKeySource(ISSUER, keyPair.getRight(), ISSUER, null));

    assertThatThrownBy(() -> this.service.auth(token)).isInstanceOf(JwtAuthException.class);
    TimeUnit.SECONDS.sleep(2);
    final Map<String, Claim> claims = this.service.auth(token);

    assertThat(claims.get(CLAIM_ISSUER).asString()).isEqualTo(ISSUER);
    assertThat(claims.get(CLAIM_NOT_BEFORE).asLong() * 1000L).isLessThan(new Date().getTime());
    assertThat(claims.get(CLAIM_EXPIRE).asLong() * 1000L).isGreaterThan(new Date().getTime());
  }

  @Test
  void validTokenWithKeyIdAndIssuerAndWillExpire() throws InterruptedException {
    final Pair<RSAPrivateKey, RSAPublicKey> keyPair = createKeyPair(RSA_PRIVATE_KEY);
    String token = createToken(keyPair, ISSUER, KEY_ID, 0, 2);
    keyLoader.addKeySource(new JwksTestKeySource(ISSUER, keyPair.getRight(), ISSUER, KEY_ID));
    final Map<String, Claim> claims = this.service.auth(token);
    assertThat(claims.get(CLAIM_ISSUER).asString()).isEqualTo(ISSUER);
    assertThat(claims.get(CLAIM_NOT_BEFORE).asLong() * 1000L).isLessThan(new Date().getTime());
    assertThat(claims.get(CLAIM_EXPIRE).asLong() * 1000L).isGreaterThan(new Date().getTime());
    TimeUnit.SECONDS.sleep(3);
    assertThatThrownBy(() -> this.service.auth(token)).isInstanceOf(JwtAuthException.class);
  }

  @Test
  void validTokenWithIssuerAndWillExpire() throws InterruptedException {
    final Pair<RSAPrivateKey, RSAPublicKey> keyPair = createKeyPair(RSA_PRIVATE_KEY);
    String token = createToken(keyPair, ISSUER, null, 0, 2);
    keyLoader.addKeySource(new JwksTestKeySource(ISSUER, keyPair.getRight(), ISSUER, null));
    final Map<String, Claim> claims = this.service.auth(token);
    assertThat(claims.get(CLAIM_ISSUER).asString()).isEqualTo(ISSUER);
    assertThat(claims.get(CLAIM_NOT_BEFORE).asLong() * 1000L).isLessThan(new Date().getTime());
    assertThat(claims.get(CLAIM_EXPIRE).asLong() * 1000L).isGreaterThan(new Date().getTime());
    TimeUnit.SECONDS.sleep(3);
    assertThatThrownBy(() -> this.service.auth(token)).isInstanceOf(JwtAuthException.class);
  }

  @Test
  void validTokenWithKeyIdAndIssuerAndConfiguredRequiredIssuer() {
    final Pair<RSAPrivateKey, RSAPublicKey> keyPair = createKeyPair(RSA_PRIVATE_KEY);
    String token = createToken(keyPair, ISSUER, KEY_ID, 0, 30);
    keyLoader.addKeySource(new JwksTestKeySource(ISSUER, keyPair.getRight(), ISSUER, KEY_ID));

    final Map<String, Claim> claims = this.service.auth(token);

    assertThat(claims.get(CLAIM_ISSUER).asString()).isEqualTo(ISSUER);
    assertThat(claims.get(CLAIM_NOT_BEFORE).asLong() * 1000L).isLessThan(new Date().getTime());
    assertThat(claims.get(CLAIM_EXPIRE).asLong() * 1000L).isGreaterThan(new Date().getTime());
  }

  @Test
  void validTokenWithIssuerAndConfiguredRequiredIssuer() {
    final Pair<RSAPrivateKey, RSAPublicKey> keyPair = createKeyPair(RSA_PRIVATE_KEY);
    String token = createToken(keyPair, ISSUER, null, 0, 30);
    keyLoader.addKeySource(new JwksTestKeySource(ISSUER, keyPair.getRight(), ISSUER, null));

    final Map<String, Claim> claims = this.service.auth(token);

    assertThat(claims.get(CLAIM_ISSUER).asString()).isEqualTo(ISSUER);
    assertThat(claims.get(CLAIM_NOT_BEFORE).asLong() * 1000L).isLessThan(new Date().getTime());
    assertThat(claims.get(CLAIM_EXPIRE).asLong() * 1000L).isGreaterThan(new Date().getTime());
  }

  @Test
  void validTokenWithKeyIdAndWrongIssuerAndCorrectConfiguredRequiredIssuer() {
    final Pair<RSAPrivateKey, RSAPublicKey> keyPair = createKeyPair(RSA_PRIVATE_KEY);
    String token = createToken(keyPair, "https://www.google.de", KEY_ID, 0, 30);
    keyLoader.addKeySource(new JwksTestKeySource(ISSUER, keyPair.getRight(), ISSUER, KEY_ID));

    assertThatThrownBy(() -> this.service.auth(token)).isInstanceOf(JwtAuthException.class);
  }

  @Test
  void validTokenWithWrongIssuerAndCorrectConfiguredRequiredIssuer() {
    final Pair<RSAPrivateKey, RSAPublicKey> keyPair = createKeyPair(RSA_PRIVATE_KEY);
    String token = createToken(keyPair, "https://www.google.de", null, 0, 30);
    keyLoader.addKeySource(new JwksTestKeySource(ISSUER, keyPair.getRight(), ISSUER, null));

    assertThatThrownBy(() -> this.service.auth(token)).isInstanceOf(JwtAuthException.class);
  }

  @Test
  void validTokenWithKeyIdAndCorrectIssuerAndWrongConfiguredRequiredIssuer() {
    final Pair<RSAPrivateKey, RSAPublicKey> keyPair = createKeyPair(RSA_PRIVATE_KEY);
    String token = createToken(keyPair, ISSUER, KEY_ID, 0, 30);
    keyLoader.addKeySource(
        new JwksTestKeySource(ISSUER, keyPair.getRight(), "https://www.google.de", KEY_ID));

    assertThatThrownBy(() -> this.service.auth(token)).isInstanceOf(JwtAuthException.class);
  }

  @Test
  void validTokenWithCorrectIssuerAndWrongConfiguredRequiredIssuer() {
    final Pair<RSAPrivateKey, RSAPublicKey> keyPair = createKeyPair(RSA_PRIVATE_KEY);
    String token = createToken(keyPair, ISSUER, null, 0, 30);
    keyLoader.addKeySource(
        new JwksTestKeySource(ISSUER, keyPair.getRight(), "https://www.google.de", null));

    assertThatThrownBy(() -> this.service.auth(token)).isInstanceOf(JwtAuthException.class);
  }

  @Test
  void validTokenWithKeyIdAndCorrectIssuerAndSameJwksSourceHostButDifferentPath() {
    final Pair<RSAPrivateKey, RSAPublicKey> keyPair = createKeyPair(RSA_PRIVATE_KEY);
    String token = createToken(keyPair, ISSUER, KEY_ID, 0, 30);
    keyLoader.addKeySource(
        new JwksTestKeySource(ISSUER + "/subpath", keyPair.getRight(), ISSUER, KEY_ID));

    final Map<String, Claim> claims = this.service.auth(token);

    assertThat(claims.get(CLAIM_ISSUER).asString()).isEqualTo(ISSUER);
    assertThat(claims.get(CLAIM_NOT_BEFORE).asLong() * 1000L).isLessThan(new Date().getTime());
    assertThat(claims.get(CLAIM_EXPIRE).asLong() * 1000L).isGreaterThan(new Date().getTime());
  }

  @Test
  void validTokenWithCorrectIssuerAndSameJwksSourceHostButDifferentPath() {
    final Pair<RSAPrivateKey, RSAPublicKey> keyPair = createKeyPair(RSA_PRIVATE_KEY);
    String token = createToken(keyPair, ISSUER, null, 0, 30);
    keyLoader.addKeySource(
        new JwksTestKeySource(ISSUER + "/subpath", keyPair.getRight(), ISSUER, null));

    final Map<String, Claim> claims = this.service.auth(token);

    assertThat(claims.get(CLAIM_ISSUER).asString()).isEqualTo(ISSUER);
    assertThat(claims.get(CLAIM_NOT_BEFORE).asLong() * 1000L).isLessThan(new Date().getTime());
    assertThat(claims.get(CLAIM_EXPIRE).asLong() * 1000L).isGreaterThan(new Date().getTime());
  }

  /**
   * Create a jwt token signed by the provided keypair.
   *
   * @param keyPair The keypair to sign the token.
   * @param issuer The issuer of the token
   * @param keyId the keyid for the jwt token
   * @param validAfterGivenSeconds the minimum passed time, when this token ia going to be valid.
   * @param expiresInGivenSeconds the maximum time is seconds until this jwt gets expired.
   * @return The newly generated jwt token.
   */
  private String createToken(
      Pair<RSAPrivateKey, RSAPublicKey> keyPair,
      String issuer,
      String keyId,
      int validAfterGivenSeconds,
      int expiresInGivenSeconds) {
    final Date currentDate = new Date();
    if (StringUtils.isNotBlank(keyId)) {
      return JWT.create()
          .withExpiresAt(DateUtils.addSeconds(currentDate, expiresInGivenSeconds))
          .withIssuer(issuer)
          .withNotBefore(DateUtils.addSeconds(currentDate, validAfterGivenSeconds))
          .withKeyId(keyId)
          .sign(Algorithm.RSA256(keyPair.getRight(), keyPair.getLeft()));
    }
    return JWT.create()
        .withExpiresAt(DateUtils.addSeconds(currentDate, expiresInGivenSeconds))
        .withIssuer(issuer)
        .withNotBefore(DateUtils.addSeconds(currentDate, validAfterGivenSeconds))
        .sign(Algorithm.RSA256(keyPair.getRight(), keyPair.getLeft()));
  }

  /**
   * Load a private key from the provided file location and generate the matching public key and
   * returns the two keys as {@link Pair}.
   *
   * @param privateKeyFileLocation The location of the private key file.
   * @return the {@link Pair} containing the private and public key.
   */
  private Pair<RSAPrivateKey, RSAPublicKey> createKeyPair(String privateKeyFileLocation) {
    try {
      final String privateKeyLocation = ResourceHelpers.resourceFilePath(privateKeyFileLocation);

      RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) loadPrivateKey(privateKeyLocation);
      RSAPublicKeySpec publicKeySpec =
          new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent());
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);

      return Pair.of(privateKey, publicKey);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Load a private key from a binary file
   *
   * @param privateKeyLocation the path of the key file as {@link String}
   * @return a {@link RSAPrivateKey} created from the binary key file.
   */
  private RSAPrivateKey loadPrivateKey(String privateKeyLocation) {
    byte[] keyBytes;
    try {
      keyBytes = Files.readAllBytes(Paths.get(privateKeyLocation));
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      return (RSAPrivateKey) kf.generatePrivate(spec);
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }
}
