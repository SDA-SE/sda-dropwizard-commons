package org.sdase.commons.server.morphia.internal;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SslUtilTest {

   @Test
   public void readCustomCa() throws Exception {
      String pemContent = IOUtils.toString(getClass().getResourceAsStream("/test.pem"));

      KeyStore truststore = SslUtil.createTruststoreFromPemKey(pemContent);

      assertThat(truststore).isNotNull();
      List<Certificate> certificates = extractCertificates(truststore);
      assertThat(certificates).hasSize(1);
   }

   @Test
   public void readLetsEncryptCaIdenTrust() throws Exception {
      String pemContent = IOUtils.toString(getClass().getResourceAsStream("/le-ca-x3-iden-trust.pem"));

      KeyStore truststore = SslUtil.createTruststoreFromPemKey(pemContent);

      assertThat(truststore).isNotNull();
      List<Certificate> certificates = extractCertificates(truststore);
      assertThat(certificates).hasSize(1);
   }

   @Test
   public void readLetsEncryptCaIsrg() throws Exception {
      String pemContent = IOUtils.toString(getClass().getResourceAsStream("/le-ca-x3-isrg-x1.pem"));

      KeyStore truststore = SslUtil.createTruststoreFromPemKey(pemContent);

      assertThat(truststore).isNotNull();
      List<Certificate> certificates = extractCertificates(truststore);
      assertThat(certificates).hasSize(1);
   }

   @Test
   public void readCustomCaBase64() throws Exception {
      String pemContent = IOUtils.toString(getClass().getResourceAsStream("/test.pem"));
      pemContent = encodeBase64(pemContent);

      KeyStore truststore = SslUtil.createTruststoreFromBase64EncodedPemKey(pemContent);

      assertThat(truststore).isNotNull();
      List<Certificate> certificates = extractCertificates(truststore);
      assertThat(certificates).hasSize(1);
   }

   @Test
   public void readLetsEncryptCaIdenTrustBase64() throws Exception {
      String pemContent = IOUtils.toString(getClass().getResourceAsStream("/le-ca-x3-iden-trust.pem"));
      pemContent = encodeBase64(pemContent);

      KeyStore truststore = SslUtil.createTruststoreFromBase64EncodedPemKey(pemContent);

      assertThat(truststore).isNotNull();
      List<Certificate> certificates = extractCertificates(truststore);
      assertThat(certificates).hasSize(1);
   }

   @Test
   public void readLetsEncryptCaIsrgBase64() throws Exception {
      String pemContent = IOUtils.toString(getClass().getResourceAsStream("/le-ca-x3-isrg-x1.pem"));
      pemContent = encodeBase64(pemContent);

      KeyStore truststore = SslUtil.createTruststoreFromBase64EncodedPemKey(pemContent);

      assertThat(truststore).isNotNull();
      List<Certificate> certificates = extractCertificates(truststore);
      assertThat(certificates).hasSize(1);
   }

   @Test
   public void readLetsEncryptIsrgCertificateFromString() throws Exception {
      String pemContent = IOUtils.toString(getClass().getResourceAsStream("/le-ca-x3-isrg-x1.pem"));

      KeyStore truststore = SslUtil.createTruststoreFromPemKey(pemContent);

      assertThat(truststore).isNotNull();
      List<Certificate> certificates = extractCertificates(truststore);
      assertThat(certificates).hasSize(1);
   }

   @Test
   public void createJoinedTrustStore() throws Exception {
      String pemContentIsrg = IOUtils.toString(getClass().getResourceAsStream("/le-ca-x3-isrg-x1.pem"));
      String pemContentIden = IOUtils.toString(getClass().getResourceAsStream("/le-ca-x3-iden-trust.pem"));
      String pemContentIdenBase64 = encodeBase64(pemContentIden);


      KeyStore truststoreIsrg = SslUtil.createTruststoreFromPemKey(pemContentIsrg);
      KeyStore truststoreIden = SslUtil.createTruststoreFromBase64EncodedPemKey(pemContentIdenBase64);
      KeyStore keyStore = SslUtil.joinKeyStores(truststoreIsrg, truststoreIden);

      assertThat(keyStore).isNotNull();
      List<Certificate> certificates = extractCertificates(keyStore);
      assertThat(certificates)
            .containsExactlyInAnyOrder(
                  extractCertificates(truststoreIsrg).get(0),
                  extractCertificates(truststoreIden).get(0)
            );
   }

   private String encodeBase64(String fullPemCertificate) {
      return Base64.getEncoder().encodeToString(fullPemCertificate.getBytes());
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