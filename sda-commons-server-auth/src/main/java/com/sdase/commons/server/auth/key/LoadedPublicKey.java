package com.sdase.commons.server.auth.key;

import java.security.interfaces.RSAPublicKey;
import java.util.Objects;

public class LoadedPublicKey {

   /**
    * The key id passed as {@code kid} with a JWT
    */
   private String kid;

   /**
    * The public key that has been loaded.
    */
   private RSAPublicKey publicKey;

   /**
    * The source where the key has been loaded from.
    */
   private KeySource keySource;

   public LoadedPublicKey(String kid, RSAPublicKey publicKey, KeySource keySource) {
      this.kid = kid;
      this.publicKey = publicKey;
      this.keySource = keySource;
   }

   public String getKid() {
      return kid;
   }

   public RSAPublicKey getPublicKey() {
      return publicKey;
   }

   public KeySource getKeySource() {
      return keySource;
   }


   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      LoadedPublicKey that = (LoadedPublicKey) o;
      return Objects.equals(kid, that.kid) &&
            Objects.equals(publicKey, that.publicKey) &&
            Objects.equals(keySource, that.keySource);
   }

   @Override
   public int hashCode() {
      return Objects.hash(kid, publicKey, keySource);
   }
}
