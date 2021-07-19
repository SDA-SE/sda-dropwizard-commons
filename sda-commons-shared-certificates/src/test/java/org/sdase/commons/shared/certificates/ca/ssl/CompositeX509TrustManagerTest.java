package org.sdase.commons.shared.certificates.ca.ssl;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Optional;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.bouncycastle.openssl.PEMParser;
import org.junit.Before;
import org.junit.Test;
import sun.security.validator.ValidatorException;

public class CompositeX509TrustManagerTest {
  public static final String AUTH_TYPE = "Basic";
  private X509TrustManager trustManager;
  private X509Certificate trustedChain;
  private X509Certificate unTrustedChain;

  @Before
  public void setUp() throws Exception {
    trustManager = createTrustManager();

    trustedChain = getCertificateFromPemFile("trusted.pem");
    unTrustedChain = getCertificateFromPemFile("untrusted.pem");
  }

  @Test
  public void shouldCreateCompositeTrustManager() {
    CompositeX509TrustManager compositeX509TrustManager =
        new CompositeX509TrustManager(Collections.singletonList(trustManager));

    assertThat(compositeX509TrustManager).isNotNull();
  }

  @Test
  public void shouldTrustServer() {
    CompositeX509TrustManager compositeX509TrustManager =
        new CompositeX509TrustManager(Collections.singletonList(trustManager));

    assertThatNoException()
        .isThrownBy(
            () -> trustManager.checkServerTrusted(new X509Certificate[] {trustedChain}, AUTH_TYPE));

    assertThatNoException()
        .isThrownBy(
            () ->
                compositeX509TrustManager.checkServerTrusted(
                    new X509Certificate[] {trustedChain}, AUTH_TYPE));
  }

  @Test
  public void shouldTrustClient() {
    CompositeX509TrustManager compositeX509TrustManager =
        new CompositeX509TrustManager(Collections.singletonList(trustManager));

    assertThatNoException()
        .isThrownBy(
            () -> trustManager.checkClientTrusted(new X509Certificate[] {trustedChain}, AUTH_TYPE));
    assertThatNoException()
        .isThrownBy(
            () ->
                compositeX509TrustManager.checkClientTrusted(
                    new X509Certificate[] {trustedChain}, AUTH_TYPE));
  }

  @Test
  public void shouldNotTrustClient() {
    CompositeX509TrustManager compositeX509TrustManager =
        new CompositeX509TrustManager(Collections.singletonList(trustManager));

    assertThatExceptionOfType(ValidatorException.class)
        .isThrownBy(
            () ->
                trustManager.checkServerTrusted(new X509Certificate[] {unTrustedChain}, AUTH_TYPE));
    assertThatExceptionOfType(CertificateException.class)
        .isThrownBy(
            () ->
                compositeX509TrustManager.checkServerTrusted(
                    new X509Certificate[] {unTrustedChain}, AUTH_TYPE));
  }

  @Test
  public void shouldNotTrustServer() {
    CompositeX509TrustManager compositeX509TrustManager =
        new CompositeX509TrustManager(Collections.singletonList(trustManager));

    assertThatExceptionOfType(CertificateException.class)
        .isThrownBy(
            () ->
                trustManager.checkServerTrusted(new X509Certificate[] {unTrustedChain}, AUTH_TYPE));
    assertThatExceptionOfType(CertificateException.class)
        .isThrownBy(
            () ->
                compositeX509TrustManager.checkServerTrusted(
                    new X509Certificate[] {unTrustedChain}, AUTH_TYPE));
  }

  @Test
  public void shouldHaveSameAcceptedIssuers() {
    CompositeX509TrustManager compositeX509TrustManager =
        new CompositeX509TrustManager(Collections.singletonList(trustManager));

    assertThat(trustManager.getAcceptedIssuers()[0].getIssuerDN())
        .isEqualTo(compositeX509TrustManager.getAcceptedIssuers()[0].getIssuerDN());
  }

  private static String readPemContent(String pemResource) {
    try {
      return new String(Files.readAllBytes(Paths.get(resourceFilePath(pemResource))));

    } catch (IOException e) {
      throw new UncheckedIOException(String.format("Failed to read %s", pemResource), e);
    }
  }

  private static X509Certificate getCertificateFromPemFile(String pemPath)
      throws CertificateException, IOException {
    return SslUtil.parseCert(new PEMParser(new StringReader(readPemContent(pemPath))));
  }

  private X509TrustManager createTrustManager() {
    CertificateReader certificateReader =
        new CertificateReader(Paths.get("src", "test", "resources").toString().concat("/notEmpty"));
    Optional<String> pemContent = certificateReader.readCertificates();

    String defaultTrustManagerAlgorithm = TrustManagerFactory.getDefaultAlgorithm();

    KeyStore truststoreFromPemKey = SslUtil.createTruststoreFromPemKey(pemContent.get());

    return SslUtil.getTrustManager(defaultTrustManagerAlgorithm, truststoreFromPemKey);
  }
}
