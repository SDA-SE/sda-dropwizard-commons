package org.sdase.commons.server.auth.key;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
      if (isPublicKey(pemKeyLocation)) {
        RSAPublicKey publicKey = loadPublicKey(pemKeyLocation);
        return Collections.singletonList(new LoadedPublicKey(kid, publicKey, this));
      } else {
        X509Certificate cer = loadCertificate(pemKeyLocation);
        RSAPublicKey publicKey = extractRsaPublicKeyFromCertificate(cer);
        LOG.info("Loaded public key for token signature verification from PEM {}", pemKeyLocation);
        return Collections.singletonList(new LoadedPublicKey(kid, publicKey, this));
      }
    } catch (IOException | CertificateException | NullPointerException | ClassCastException e) {

      throw new KeyLoadFailedException(
          "Failed to load public key for token signature verification from PEM " + pemKeyLocation,
          e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PemKeySource that = (PemKeySource) o;
    return Objects.equals(kid, that.kid) && Objects.equals(pemKeyLocation, that.pemKeyLocation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kid, pemKeyLocation);
  }

  @Override
  public String toString() {
    return "PemKeySource{" + "kid='" + kid + '\'' + ", pemKeyLocation=" + pemKeyLocation + '}';
  }

  private X509Certificate loadCertificate(URI location) throws CertificateException, IOException {
    try (InputStream is = location.toURL().openStream()) {
      CertificateFactory fact = CertificateFactory.getInstance("X.509");
      Certificate certificate = fact.generateCertificate(is);
      if (certificate instanceof X509Certificate) {
        return (X509Certificate) certificate;
      }
      throw new KeyLoadFailedException(
          "Only X509Certificate certificates are supported but loaded a "
              + certificate.getClass()
              + " from "
              + pemKeyLocation);
    }
  }

  private RSAPublicKey extractRsaPublicKeyFromCertificate(X509Certificate certificate)
      throws KeyLoadFailedException { // NOSONAR
    RSAPublicKey publicKey;
    PublicKey cerPublicKey = certificate.getPublicKey();
    if (cerPublicKey instanceof RSAPublicKey) {
      publicKey = (RSAPublicKey) cerPublicKey;
    } else {
      throw new KeyLoadFailedException(
          "Only RSA keys are supported but loaded a "
              + cerPublicKey.getClass()
              + " from "
              + pemKeyLocation);
    }
    return publicKey;
  }

  private boolean isPublicKey(URI pemKeyLocation) {
    try (InputStream is = pemKeyLocation.toURL().openStream()) {
      String keyContent = readContent(is).trim();
      return keyContent.startsWith("-----BEGIN PUBLIC KEY-----")
          && keyContent.endsWith("-----END PUBLIC KEY-----");
    } catch (IOException e) {
      throw new KeyLoadFailedException("Failed to read key from " + pemKeyLocation, e);
    }
  }

  private RSAPublicKey loadPublicKey(URI pemKeyLocation) {
    LOG.info("Loading public key for token signature verification from PEM {}", pemKeyLocation);
    try (InputStream is = pemKeyLocation.toURL().openStream()) {
      String pemPublicKeyContent = readContent(is);
      String publicKeyPem =
          pemPublicKeyContent
              .replace("-----BEGIN PUBLIC KEY-----", "")
              .replaceAll("(\\r\\n|\\r|\\n)", "")
              .replace("-----END PUBLIC KEY-----", "");

      byte[] encoded = Base64.decodeBase64(publicKeyPem);
      X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
      final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    } catch (ClassCastException
        | IOException
        | NoSuchAlgorithmException
        | InvalidKeySpecException e) {
      throw new KeyLoadFailedException("Failed to load public key at " + pemKeyLocation, e);
    }
  }

  private String readContent(InputStream content) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int bytesRead;
    byte[] data = new byte[1024];
    while ((bytesRead = content.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, bytesRead);
    }
    return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
  }
}
