package org.sdase.commons.server.auth.key;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Loads public keys from a
 * <a href="https://tools.ietf.org/html/draft-ietf-jose-json-web-key-41#section-5">JSON Web Key Set</a>.
 */
public class JwksKeySource implements KeySource {

   private String jwksUri;

   private Client client;

   /**
    * @param jwksUri the uri providing a
    *                <a href="https://tools.ietf.org/html/draft-ietf-jose-json-web-key-41#section-5">JSON Web Key Set</a>
    *                as Json, e.g. {@code http://keycloak.example.com/auth/realms/sda-reference-solution/protocol/openid-connect/certs}
    * @param client the client used to execute the discovery request, may be created from the application
    *               {@link io.dropwizard.setup.Environment} using {@link io.dropwizard.client.JerseyClientBuilder}
    */
   public JwksKeySource(String jwksUri, Client client) {
      this.jwksUri = jwksUri;
      this.client = client;
   }

   @Override
   public List<LoadedPublicKey> loadKeysFromSource() {
      try {
         Jwks jwks = client.target(jwksUri).request(MediaType.APPLICATION_JSON)
               .get(Jwks.class);
         return jwks.getKeys().stream()
               .filter(Objects::nonNull)
               .filter(this::isForSigning)
               .filter(this::isRsaKeyType)
               .filter(this::isRsa256Key)
               .map(this::toPublicKey)
               .collect(Collectors.toList());
      } catch (Exception e) {
         throw new KeyLoadFailedException(e);
      }
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      JwksKeySource keySource = (JwksKeySource) o;
      return Objects.equals(jwksUri, keySource.jwksUri) &&
            Objects.equals(client, keySource.client);
   }

   @Override
   public int hashCode() {
      return Objects.hash(jwksUri, client);
   }

   @Override
   public String toString() {
      return "JwksKeySource{" +
            "jwksUri='" + jwksUri + '\'' +
            '}';
   }

   private boolean isForSigning(Key key) {
      return StringUtils.isBlank(key.getUse()) || "sig".equals(key.getUse());
   }

   private boolean isRsaKeyType(Key key) {
      return "RSA".equals(key.getKty());
   }

   private boolean isRsa256Key(Key key) {
      // We only support RSA256, if blank we assume it to be RS256.
      return StringUtils.isBlank(key.getAlg()) || "RS256".equals(key.getAlg());
   }

   private LoadedPublicKey toPublicKey(Key key) throws KeyLoadFailedException { // NOSONAR
      try {
         String kid = key.getKid();
         String keyType = key.getKty();
         KeyFactory keyFactory = KeyFactory.getInstance(keyType);

         BigInteger modulus = readBase64AsBigInt(key.getN());
         BigInteger exponent = readBase64AsBigInt(key.getE());

         PublicKey publicKey = keyFactory.generatePublic(new RSAPublicKeySpec(modulus, exponent));
         if (publicKey instanceof RSAPublicKey) {
            return new LoadedPublicKey(kid, (RSAPublicKey) publicKey, this);
         } else {
            throw new KeyLoadFailedException("Only RSA keys are supported but loaded a " + publicKey.getClass()
                  + " from " + jwksUri);
         }
      } catch (NullPointerException | InvalidKeySpecException | NoSuchAlgorithmException e) {
         throw new KeyLoadFailedException(e);
      }

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
      private String alg;
      private String use;
      private String n;
      private String e;

      public String getKid() {
         return kid;
      }

      public Key setKid(String kid) {
         this.kid = kid;
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
   }

}
