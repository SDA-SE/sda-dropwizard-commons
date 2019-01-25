package org.sdase.commons.server.morphia.internal;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.X509TrustedCertificateBlock;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Enumeration;

/**
 * Utility to help with SSL related stuff according to the {@code MorphiaBundle}
 */
class SslUtil {
   private SslUtil() {
      // this is a utility
   }

   static SSLContext createSslContext(KeyStore keyStore)
         throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
      String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
      TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm);
      trustManagerFactory.init(keyStore);

      SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
      sslContext.init(null, trustManagerFactory.getTrustManagers(), SecureRandom.getInstanceStrong());

      return sslContext;
   }

   static KeyStore joinKeyStores(KeyStore... keyStores)
         throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
      KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(null, null);
      int i = 0;
      for (KeyStore store : keyStores) {
         if (store == null) {
            continue;
         }
         Enumeration<String> aliases = store.aliases();
         while (aliases.hasMoreElements()) {
            keyStore.setCertificateEntry("cert_" + i, store.getCertificate(aliases.nextElement()));
            i++;
         }
      }
      return keyStore;
   }

   static KeyStore createTruststoreFromPemKey(String certificateAsString)
         throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
      try (PEMParser parser = new PEMParser(new StringReader(certificateAsString))) {
         KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
         keyStore.load(null, null);
         int i = 0;
         X509Certificate certificate;
         while ((certificate = parseCert(parser)) != null) {
            keyStore.setCertificateEntry("cert_" + i, certificate);
            i += 1;
         }
         return keyStore;
      }
   }

   static KeyStore createTruststoreFromBase64EncodedPemKey(String base64EncodedCertificateAsString)
         throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
      byte[] base64EncodedCertificateBytes = base64EncodedCertificateAsString.getBytes();
      byte[] certificateBytes = Base64.getDecoder().decode(base64EncodedCertificateBytes);
      String certificateAsString = new String(certificateBytes, StandardCharsets.UTF_8);
      return createTruststoreFromPemKey(certificateAsString);
   }

   private static X509Certificate parseCert(PEMParser parser) throws IOException, CertificateException {

      Object certificateObject = parser.readObject();
      if (certificateObject == null) {
         return null;
      }
      if (certificateObject instanceof X509CertificateHolder) {
         X509CertificateHolder certHolder = (X509CertificateHolder) certificateObject;
         return new JcaX509CertificateConverter().getCertificate(certHolder);
      }
      if (certificateObject instanceof X509TrustedCertificateBlock) {
         X509CertificateHolder certHolder = ((X509TrustedCertificateBlock) certificateObject).getCertificateHolder();
         return new JcaX509CertificateConverter().getCertificate(certHolder);
      }
      throw new CertificateException("Could not read certificate of type " + certificateObject.getClass());
   }
}
