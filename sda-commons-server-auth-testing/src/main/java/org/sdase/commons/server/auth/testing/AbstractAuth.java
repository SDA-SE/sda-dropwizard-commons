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

public class AbstractAuth {

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
