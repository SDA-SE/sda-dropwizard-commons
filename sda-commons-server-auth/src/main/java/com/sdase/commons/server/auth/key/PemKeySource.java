package com.sdase.commons.server.auth.key;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PemKeySource implements KeySource {

   private static final Logger LOG = LoggerFactory.getLogger(PemKeySource.class);

   private String kid;

   private URI pemKeyLocation;

   public PemKeySource(String kid, URI pemKeyLocation) {
      this.kid = kid;
      this.pemKeyLocation = pemKeyLocation;
   }

   @Override
   public List<LoadedPublicKey> loadKeysFromSource() {
      try {
         LOG.info("Loading public key for token signature verification from PEM {}", pemKeyLocation);
         X509Certificate cer = loadCertificate(pemKeyLocation);
         RSAPublicKey publicKey = extractRsaPublicKey(cer);
         LOG.info("Loaded public key for token signature verification from PEM {}", pemKeyLocation);
         return Collections.singletonList(new LoadedPublicKey(kid, publicKey, this));
      } catch (IOException | CertificateException | NullPointerException | ClassCastException e) {
         LOG.error("Failed to load public key for token signature verification from PEM {}", pemKeyLocation, e);
         throw new KeyLoadFailedException(e.getMessage(), e);
      }
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      PemKeySource that = (PemKeySource) o;
      return Objects.equals(kid, that.kid) &&
            Objects.equals(pemKeyLocation, that.pemKeyLocation);
   }

   @Override
   public int hashCode() {
      return Objects.hash(kid, pemKeyLocation);
   }

   private X509Certificate loadCertificate(URI location) throws CertificateException, IOException {
      try (InputStream is = location.toURL().openStream()) {
         CertificateFactory fact = CertificateFactory.getInstance("X.509");
         Certificate certificate = fact.generateCertificate(is);
         if (certificate instanceof X509Certificate) {
            return (X509Certificate) certificate;
         }
         throw new KeyLoadFailedException("Only X509Certificate certificates are supported but loaded a "
               + certificate.getClass() + " from " + pemKeyLocation);
      }
   }

   private RSAPublicKey extractRsaPublicKey(X509Certificate certificate) throws KeyLoadFailedException { // NOSONAR
      RSAPublicKey publicKey;
      PublicKey cerPublicKey = certificate.getPublicKey();
      if (cerPublicKey instanceof RSAPublicKey) {
         publicKey = (RSAPublicKey) cerPublicKey;
      }
      else {
         throw new KeyLoadFailedException("Only RSA keys are supported but loaded a " + cerPublicKey.getClass()
               + " from " + pemKeyLocation);
      }
      return publicKey;
   }

}
