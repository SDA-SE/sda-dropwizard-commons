package org.sdase.commons.shared.certificates.ca.ssl;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.net.ssl.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.X509TrustedCertificateBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SslUtil {

  private static final Logger LOG = LoggerFactory.getLogger(SslUtil.class);

  private static final String DEFAULT_SSL_PROTOCOL = "TLSv1.2";

  private SslUtil() {}

  public static SSLContext createSslContext(KeyStore keystore) {
    try {
      TrustManager[] trustManagers = {createCompositeTrustManager(keystore)};

      // create sslContext combining multi managers
      SSLContext sslContext = SSLContext.getInstance(DEFAULT_SSL_PROTOCOL);
      sslContext.init(null, trustManagers, createSecureRandom());
      return sslContext;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static KeyStore createTruststoreFromPemKey(String certificateAsString) {
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
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static X509Certificate parseCert(PEMParser parser)
      throws IOException, CertificateException {

    Object certificateObject = parser.readObject();
    if (certificateObject == null) {
      return null;
    }
    if (certificateObject instanceof X509CertificateHolder) {
      X509CertificateHolder certHolder = (X509CertificateHolder) certificateObject;
      return new JcaX509CertificateConverter().getCertificate(certHolder);
    }
    if (certificateObject instanceof X509TrustedCertificateBlock) {
      X509CertificateHolder certHolder =
          ((X509TrustedCertificateBlock) certificateObject).getCertificateHolder();
      return new JcaX509CertificateConverter().getCertificate(certHolder);
    }
    throw new CertificateException(
        "Could not read certificate of type " + certificateObject.getClass());
  }

  public static SecureRandom createSecureRandom() throws NoSuchAlgorithmException {
    String algorithmNativePRNG = "NativePRNG";
    String algorithmWindowsPRNG = "Windows-PRNG";
    try {
      return SecureRandom.getInstance(algorithmNativePRNG);
    } catch (NoSuchAlgorithmException e) {
      LOG.warn(
          "Failed to create SecureRandom with algorithm {}. Falling back to {}."
              + "This should only happen on windows machines.",
          algorithmNativePRNG,
          algorithmWindowsPRNG,
          e);
      return SecureRandom.getInstance(algorithmWindowsPRNG);
    }
  }

  public static X509TrustManager getTrustManager(String algorithm, KeyStore keyStore) {
    try {
      TrustManagerFactory factory = TrustManagerFactory.getInstance(algorithm);
      factory.init(keyStore);
      return Arrays.stream(factory.getTrustManagers())
          .filter(X509TrustManager.class::isInstance)
          .map(X509TrustManager.class::cast)
          .findFirst()
          .orElse(null);
    } catch (Exception e) {
      // nothing here
    }
    return null;
  }

  private static TrustManager createCompositeTrustManager(KeyStore keystore) {
    String defaultTrustManagerAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
    X509TrustManager defaultTrustManager = getTrustManager(defaultTrustManagerAlgorithm, null);
    X509TrustManager customTrustManager = getTrustManager(defaultTrustManagerAlgorithm, keystore);
    return new CompositeX509TrustManager(Arrays.asList(defaultTrustManager, customTrustManager));
  }
}
