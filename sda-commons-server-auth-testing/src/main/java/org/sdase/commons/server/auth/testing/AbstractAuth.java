package org.sdase.commons.server.auth.testing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import org.sdase.commons.server.auth.config.AuthConfig;
import org.sdase.commons.server.auth.config.KeyLocation;
import org.sdase.commons.server.auth.config.KeyUriType;

public class AbstractAuth {

  protected final boolean disableAuth;

  protected final String keyId;

  protected final String issuer;

  protected final String subject;

  protected RSAPrivateKey privateKey;

  protected final String privateKeyLocation;

  protected final String certificateLocation;

  protected AuthConfig authConfig;

  public AbstractAuth(boolean disableAuth, String keyId, String issuer, String subject,
      String privateKeyLocation, String certificateLocation) {
    this.disableAuth = disableAuth;
    this.keyId = keyId;
    this.issuer = issuer;
    this.subject = subject;
    this.privateKeyLocation = privateKeyLocation;
    this.certificateLocation = certificateLocation;
  }

  protected RSAPrivateKey loadPrivateKey(String privateKeyLocation) {
    try (InputStream is = URI.create(privateKeyLocation).toURL().openStream()) {
      byte[] privateKeyBytes = read(is);
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKeyBytes);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return (RSAPrivateKey) keyFactory.generatePrivate(spec);
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      return null;
    }
  }

  protected KeyLocation createKeyLocation() {
    KeyLocation keyLocation = new KeyLocation();
    keyLocation.setPemKeyId(keyId);
    keyLocation.setLocation(URI.create(certificateLocation));
    keyLocation.setType(KeyUriType.PEM);
    return keyLocation;
  }

  private byte[] read(InputStream is) throws IOException {
    try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      int nRead;
      byte[] data = new byte[1024];
      while ((nRead = is.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }
      return buffer.toByteArray();
    }
  }
}
