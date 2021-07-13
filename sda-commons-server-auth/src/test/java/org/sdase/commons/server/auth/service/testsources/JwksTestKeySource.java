package org.sdase.commons.server.auth.service.testsources;

import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.List;
import org.sdase.commons.server.auth.key.JwksKeySource;
import org.sdase.commons.server.auth.key.KeyLoadFailedException;
import org.sdase.commons.server.auth.key.LoadedPublicKey;

public class JwksTestKeySource extends JwksKeySource {

  private final RSAPublicKey publicKey;
  private final String requiredIssuer;
  private final String kid;

  public JwksTestKeySource(
      String jwksUri, RSAPublicKey publicKey, String requiredIssuer, String kid) {
    super(jwksUri, null, requiredIssuer);
    this.publicKey = publicKey;
    this.requiredIssuer = requiredIssuer;
    this.kid = kid;
  }

  @Override
  public List<LoadedPublicKey> loadKeysFromSource() throws KeyLoadFailedException {
    return Collections.singletonList(new LoadedPublicKey(kid, publicKey, this, requiredIssuer));
  }
}
