package org.sdase.commons.shared.certificates.ca;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManagerFactory;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.routing.RoutingSupport;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.HttpException;
import org.junit.jupiter.api.Test;
import org.sdase.commons.shared.certificates.ca.ssl.CertificateReader;
import org.sdase.commons.shared.certificates.ca.ssl.SslUtil;

class CaCertificatesBundleHttpsIT {
  private static final String DEFAULT_SSL_PROTOCOL = "TLSv1.2";

  private static final String securedHost = "https://api.github.com";

  @Test
  void shouldFailWithCustomTrustStore() throws Exception {

    CertificateReader certificateReader =
        new CertificateReader(Paths.get("src", "test", "resources").toString());
    Optional<String> pemContent = certificateReader.readCertificates();
    assertThat(pemContent).isPresent();

    // create custom sslContext that has no trusted certificate
    SSLContext sslContext = createSSlContextWithoutDefaultMerging(pemContent.get());

    assertThatExceptionOfType(SSLHandshakeException.class)
        .isThrownBy(() -> callSecureEndpointWithSSLContextForStatus(sslContext));
  }

  @Test
  void shouldMakeHttpsOK200withCustomTrustStore() throws Exception {

    CertificateReader certificateReader =
        new CertificateReader(Paths.get("src", "test", "resources").toString());
    Optional<String> pemContent = certificateReader.readCertificates();
    assertThat(pemContent).isPresent();

    // create custom sslContext that has no trusted certificate but falls back to JVM default
    SSLContext sslContext =
        SslUtil.createSslContext(SslUtil.createTruststoreFromPemKey(pemContent.get()));

    assertThat(callSecureEndpointWithSSLContextForStatus(sslContext)).isEqualTo(200);
  }

  static int callSecureEndpointWithSSLContextForStatus(SSLContext sslContext) throws IOException {

    final SSLConnectionSocketFactory sslSocketFactory =
        SSLConnectionSocketFactoryBuilder.create().setSslContext(sslContext).build();
    final HttpClientConnectionManager cm =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setSSLSocketFactory(sslSocketFactory)
            .setDefaultConnectionConfig(
                ConnectionConfig.custom().setConnectTimeout(10, SECONDS).build())
            .build();

    HttpGet request = new HttpGet(securedHost);
    try (var client =
            HttpClients.custom().setConnectionManager(cm).evictExpiredConnections().build();
        var response = client.executeOpen(RoutingSupport.determineHost(request), request, null)) {
      return response.getCode();
    } catch (HttpException e) {
      throw new RuntimeException(e);
    }
  }

  private static SSLContext createSSlContextWithoutDefaultMerging(String pemContent)
      throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    KeyStore truststore = SslUtil.createTruststoreFromPemKey(pemContent);
    SSLContext sslContext = SSLContext.getInstance(DEFAULT_SSL_PROTOCOL);

    String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm);
    trustManagerFactory.init(truststore);

    sslContext.init(null, trustManagerFactory.getTrustManagers(), SslUtil.createSecureRandom());
    return sslContext;
  }
}
