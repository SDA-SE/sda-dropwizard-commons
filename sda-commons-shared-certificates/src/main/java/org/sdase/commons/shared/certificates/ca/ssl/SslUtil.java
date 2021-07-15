package org.sdase.commons.shared.certificates.ca.ssl;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.X509TrustedCertificateBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SslUtil {

  private static final Logger LOG = LoggerFactory.getLogger(SslUtil.class);

  private SslUtil() {}

  public static SSLContext createSslContext(KeyStore keyStore) {
    try {
      String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
      TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm);
      trustManagerFactory.init(keyStore);

      SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
      sslContext.init(null, trustManagerFactory.getTrustManagers(), createSecureRandom());

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

  private static X509Certificate parseCert(PEMParser parser)
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

  private static SecureRandom createSecureRandom() throws NoSuchAlgorithmException {
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
}
