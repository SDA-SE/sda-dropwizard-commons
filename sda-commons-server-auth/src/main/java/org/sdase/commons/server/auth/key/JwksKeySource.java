package org.sdase.commons.server.auth.key;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.MediaType;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads public keys from a <a
 * href="https://tools.ietf.org/html/draft-ietf-jose-json-web-key-41#section-5">JSON Web Key
 * Set</a>.
 */
public class JwksKeySource implements KeySource {

  private static final Logger LOGGER = LoggerFactory.getLogger(JwksKeySource.class);

  private static final String RSA_KTY = "RSA";
  private static final String EC_KTY = "EC";
  private static final Set<String> SUPPORTED_KTY = Set.of(RSA_KTY, EC_KTY);
  private static final Set<String> SUPPORTED_ALG =
      Set.of("RS256", "RS384", "RS512", "ES256", "ES384", "ES512");

  private final String jwksUri;

  private final Client client;

  private final String requiredIssuer;

  /**
   * @param jwksUri the uri providing a <a
   *     href="https://tools.ietf.org/html/draft-ietf-jose-json-web-key-41#section-5">JSON Web Key
   *     Set</a> as Json, e.g. {@code
   *     http://keycloak.example.com/auth/realms/sda-reference-solution/protocol/openid-connect/certs}
   * @param client the client used to execute the discovery request, may be created from the
   *     application {@link io.dropwizard.core.setup.Environment} using {@link
   *     io.dropwizard.client.JerseyClientBuilder}
   * @param requiredIssuer the required value of the issuer claim of the token in conjunction to the
   *     current key.
   */
  public JwksKeySource(String jwksUri, Client client, String requiredIssuer) {
    this.jwksUri = jwksUri;
    this.client = client;
    this.requiredIssuer = requiredIssuer;
  }

  @Override
  public List<LoadedPublicKey> loadKeysFromSource() {
    try {
      Jwks jwks = client.target(jwksUri).request(MediaType.APPLICATION_JSON).get(Jwks.class);
      return jwks.getKeys().stream()
          .filter(Objects::nonNull)
          .filter(this::isForSigning)
          .filter(this::isSupportedKeyType)
          .filter(this::isSupportedAlg)
          .map(this::toPublicKey)
          .collect(Collectors.toList());
    } catch (KeyLoadFailedException e) {
      throw e;
    } catch (WebApplicationException e) {
      try {
        e.getResponse().close();
      } catch (ProcessingException ex) {
        LOGGER.warn("Error while loading keys from JWKS while closing response", ex);
      }
      throw new KeyLoadFailedException(e);
    } catch (Exception e) {
      throw new KeyLoadFailedException(e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    JwksKeySource keySource = (JwksKeySource) o;
    return Objects.equals(jwksUri, keySource.jwksUri) && Objects.equals(client, keySource.client);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jwksUri, client);
  }

  @Override
  public String toString() {
    return "JwksKeySource{" + "jwksUri='" + jwksUri + '\'' + '}';
  }

  private boolean isForSigning(Key key) {
    return StringUtils.isBlank(key.getUse()) || "sig".equals(key.getUse());
  }

  private boolean isSupportedKeyType(Key key) {
    return key.getKty() != null && SUPPORTED_KTY.contains(key.getKty());
  }

  private boolean isSupportedAlg(Key key) {
    return key.getAlg() != null && SUPPORTED_ALG.contains(key.getAlg());
  }

  private static String mapCrvToStdName(String crv) {
    return switch (crv) {
      case "P-256" -> "secp256r1";
      case "P-384" -> "secp384r1";
      case "P-521" -> "secp521r1";
      default -> throw new KeyLoadFailedException(
          "EC keys are supported but loaded an unsupported EC curve: '" + crv + "'");
    };
  }

  private LoadedPublicKey toPublicKey(Key key) throws KeyLoadFailedException { // NOSONAR
    try {
      String keyType = key.getKty();
      KeyFactory keyFactory = KeyFactory.getInstance(keyType);
      return switch (keyType) {
        case RSA_KTY -> toRsaPublicKey(key, keyFactory);
        case EC_KTY -> toEcPublicKey(key, keyFactory);
        default -> throw new KeyLoadFailedException(
            "Unsupported key: " + key.getClass() + " from " + jwksUri);
      };
    } catch (NullPointerException
        | InvalidKeySpecException
        | NoSuchAlgorithmException
        | InvalidParameterSpecException e) {
      throw new KeyLoadFailedException(e);
    }
  }

  private LoadedPublicKey toRsaPublicKey(Key key, KeyFactory keyFactory)
      throws InvalidKeySpecException {
    BigInteger modulus = readBase64AsBigInt(key.getN());
    BigInteger exponent = readBase64AsBigInt(key.getE());
    PublicKey publicKey = keyFactory.generatePublic(new RSAPublicKeySpec(modulus, exponent));
    return new LoadedPublicKey(
        key.getKid(), key.getX5t(), publicKey, this, requiredIssuer, key.getAlg());
  }

  private LoadedPublicKey toEcPublicKey(Key key, KeyFactory keyFactory)
      throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidParameterSpecException {

    BigInteger x = readBase64AsBigInt(key.getX());
    BigInteger y = readBase64AsBigInt(key.getY());

    AlgorithmParameters parameters = AlgorithmParameters.getInstance(EC_KTY);
    parameters.init(new ECGenParameterSpec(mapCrvToStdName(key.getCrv())));

    PublicKey publicKey =
        keyFactory.generatePublic(
            new ECPublicKeySpec(
                new ECPoint(x, y), parameters.getParameterSpec(ECParameterSpec.class)));
    return new LoadedPublicKey(
        key.getKid(), key.getX5t(), publicKey, this, requiredIssuer, key.getAlg());
  }

  private static BigInteger readBase64AsBigInt(String encodedBigInt) {
    return new BigInteger(1, Base64.getUrlDecoder().decode(encodedBigInt));
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Jwks {
    private List<Key> keys;

    public List<Key> getKeys() {
      return keys;
    }

    public Jwks setKeys(List<Key> keys) {
      this.keys = keys;
      return this;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Key {
    private String kid;
    private String kty;
    private String x5t;
    private String alg;
    private String use;
    private String n;
    private String e;
    private String x;
    private String y;
    private String crv;

    public String getKid() {
      return kid;
    }

    public Key setKid(String kid) {
      this.kid = kid;
      return this;
    }

    public String getX5t() {
      return x5t;
    }

    public Key setX5t(String x5t) {
      this.x5t = x5t;
      return this;
    }

    public String getKty() {
      return kty;
    }

    public Key setKty(String kty) {
      this.kty = kty;
      return this;
    }

    public String getAlg() {
      return alg;
    }

    public Key setAlg(String alg) {
      this.alg = alg;
      return this;
    }

    public String getUse() {
      return use;
    }

    public Key setUse(String use) {
      this.use = use;
      return this;
    }

    public String getN() {
      return n;
    }

    public Key setN(String n) {
      this.n = n;
      return this;
    }

    public String getE() {
      return e;
    }

    public Key setE(String e) {
      this.e = e;
      return this;
    }

    public String getX() {
      return x;
    }

    public Key setX(String x) {
      this.x = x;
      return this;
    }

    public String getY() {
      return y;
    }

    public Key setY(String y) {
      this.y = y;
      return this;
    }

    public String getCrv() {
      return crv;
    }

    public Key setCrv(String crv) {
      this.crv = crv;
      return this;
    }
  }
}
