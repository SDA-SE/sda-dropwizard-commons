package org.sdase.commons.server.morphia.internal;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SslUtilTest {

   @Test
   public void readCustomCa() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
      String pemContent = IOUtils.toString(getClass().getResourceAsStream("/test.pem"));

      KeyStore truststore = SslUtil.createTruststoreFromPemKey(pemContent);

      assertThat(truststore).isNotNull();
      List<Certificate> certificates = extractCertificates(truststore);
      assertThat(certificates).hasSize(1);
   }

   @Test
   public void readLetsEncryptCaIdenTrust() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
      String pemContent = IOUtils.toString(getClass().getResourceAsStream("/le-ca-x3-iden-trust.pem"));

      KeyStore truststore = SslUtil.createTruststoreFromPemKey(pemContent);

      assertThat(truststore).isNotNull();
      List<Certificate> certificates = extractCertificates(truststore);
      assertThat(certificates).hasSize(1);
   }

   @Test
   public void readLetsEncryptCaIsrg() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
      String pemContent = IOUtils.toString(getClass().getResourceAsStream("/le-ca-x3-isrg-x1.pem"));

      KeyStore truststore = SslUtil.createTruststoreFromPemKey(pemContent);

      assertThat(truststore).isNotNull();
      List<Certificate> certificates = extractCertificates(truststore);
      assertThat(certificates).hasSize(1);
   }


   private List<Certificate> extractCertificates(KeyStore truststore) throws KeyStoreException {
      Enumeration<String> aliases = truststore.aliases();
      List<Certificate> certificates = new ArrayList<>();
      while (aliases.hasMoreElements()) {
         certificates.add(truststore.getCertificate(aliases.nextElement()));
      }
      return certificates;
   }
}