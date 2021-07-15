package org.sdase.commons.shared.certificates.ca.ssl;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Test;

class SslUtilTest {

  // absolute path of resources directory
  private final String resourcesDirectory = Paths.get("src", "test", "resources").toString();

  @Test
  void shouldReadCombinedCa() throws KeyStoreException {
    String pemContent = readPemContent("combined.pem");

    KeyStore truststore = SslUtil.createTruststoreFromPemKey(pemContent);

    assertThat(truststore).isNotNull();
    List<Certificate> certificates = extractCertificates(truststore);
    assertThat(certificates).hasSize(3);
  }

  @Test
  void shouldFindAllCertificatesRecursively() throws KeyStoreException {
    CertificateReader certificateReader = new CertificateReader(resourcesDirectory);

    String pemContent = certificateReader.readCertificates().get();

    KeyStore truststore = SslUtil.createTruststoreFromPemKey(pemContent);

    List<Certificate> certificates = extractCertificates(truststore);
    assertThat(truststore).isNotNull();
    assertThat(certificates).hasSize(5);
  }

  @Test
  void shouldCreateSslContext() {
    KeyStore givenTrustStore = SslUtil.createTruststoreFromPemKey(readPemContent("test.pem"));

    SSLContext sslContext = SslUtil.createSslContext(givenTrustStore);

    assertThat(sslContext).isNotNull().extracting(SSLContext::getProtocol).isEqualTo("TLSv1.2");
  }

  private String readPemContent(String pemResource) {
    try {
      return new String(Files.readAllBytes(Paths.get(resourceFilePath(pemResource))));

    } catch (IOException e) {
      throw new UncheckedIOException(String.format("Failed to read %s", pemResource), e);
    }
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
