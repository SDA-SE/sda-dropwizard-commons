package com.sdase.commons.server.auth.testing;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@code AuthBuilder} is used to build JWT authentication in test cases that is accepted by the tested application
 * if the test is initialized with the {@link AuthRule}. Properly configured instances of the {@code AuthBuilder} can be
 * created from the {@link AuthRule} using {@link AuthRule#auth()} within the test.
 */
public class AuthBuilder {

   private final RSAPublicKey publicKey;

   private final RSAPrivateKey privateKey;

   private String keyId;

   private String issuer;

   private String subject;

   private final Map<String, Object> claims = new HashMap<>();

   /**
    * Use {@link AuthRule#auth()} to create {@code AuthBuilder} instances.
    * @param keyId the {@code kid} written in the token header
    * @param publicKey the public key that verifies the token
    * @param privateKey the private key that signs the token
    */
   AuthBuilder(String keyId, RSAPublicKey publicKey, RSAPrivateKey privateKey) {
      this.keyId = keyId;
      this.publicKey = publicKey;
      this.privateKey = privateKey;
   }

   public AuthBuilder withIssuer(String issuer) {
      this.issuer = issuer;
      return this;
   }

   public AuthBuilder withSubject(String subject) {
      this.subject = subject;
      return this;
   }

   public AuthBuilder addClaim(String key, Boolean value) {
      this.claims.put(key, value);
      return this;
   }

   public AuthBuilder addClaim(String key, Integer value) {
      this.claims.put(key, value);
      return this;
   }

   public AuthBuilder addClaim(String key, Long value) {
      this.claims.put(key, value);
      return this;
   }

   public AuthBuilder addClaim(String key, String value) {
      this.claims.put(key, value);
      return this;
   }

   public AuthBuilder addClaim(String key, Integer[] value) {
      this.claims.put(key, value);
      return this;
   }

   public AuthBuilder addClaim(String key, Long[] value) {
      this.claims.put(key, value);
      return this;
   }

   public AuthBuilder addClaim(String key, String[] value) {
      this.claims.put(key, value);
      return this;
   }

   public AuthBuilder addClaim(String key, Double value) {
      this.claims.put(key, value);
      return this;
   }

   public AuthBuilder addClaim(String key, Date value) {
      this.claims.put(key, value);
      return this;
   }

   public AuthBuilder addClaims(Map<String, Object> claims) {
      if (!claims.values().stream().allMatch(this::isSupportedClaimType)) {
         throw new IllegalArgumentException("Claims contain invalid type: " + claims);
      }
      this.claims.putAll(claims);
      return this;
   }

   private boolean isSupportedClaimType(Object o) {
      return (o instanceof String)
            || (o instanceof String[])
            || (o instanceof Integer)
            || (o instanceof Integer[])
            || (o instanceof Long)
            || (o instanceof Long[])
            || (o instanceof Double)
            || (o instanceof Boolean)
            || (o instanceof Date);
   }

   /**
    * @return the signed and encoded token, e.g. {@code eyXXX.eyYYY.ZZZ}
    */
   public String buildToken() {
      Algorithm algorithm = Algorithm.RSA256(publicKey, privateKey);
      JWTCreator.Builder builder = JWT
            .create()
            .withKeyId(keyId)
            .withIssuer(issuer)
            .withSubject(subject);
      claims.keySet().forEach(key -> {
         Object value = claims.get(key);
         if (value instanceof String) {
            builder.withClaim(key, (String) value);
         } else if (value instanceof Long) {
            builder.withClaim(key, (Long) value);
         } else if (value instanceof Integer) {
            builder.withClaim(key, (Integer) value);
         } else if (value instanceof Double) {
            builder.withClaim(key, (Double) value);
         } else if (value instanceof Date) {
            builder.withClaim(key, (Date) value);
         } else if (value instanceof Boolean) {
            builder.withClaim(key, (Boolean) value);
         } else if (value instanceof String[]) {
            builder.withArrayClaim(key, (String[]) value);
         } else if (value instanceof Long[]) {
            builder.withArrayClaim(key, (Long[]) value);
         } else if (value instanceof Integer[]) {
            builder.withArrayClaim(key, (Integer[]) value);
         }
      });
      return builder.sign(algorithm);
   }

   /**
    * @return the signed and encoded token with {@code Bearer} prefix to be used directly as {@code Authorization}
    *         header value, e.g. {@code Bearer eyXXX.eyYYY.ZZZ}
    */
   public String buildHeaderValue() {
      return "Bearer " + buildToken();
   }

   /**
    * @return a map with a trusted {@code Authorization} header to be used with
    *       {@link javax.ws.rs.client.Invocation.Builder#headers(MultivaluedMap)}
    */
   public MultivaluedMap<String, Object> buildAuthHeader() {
      MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
      headers.add(HttpHeaders.AUTHORIZATION, buildHeaderValue());
      return headers;
   }

}
