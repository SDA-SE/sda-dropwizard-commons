package org.sdase.commons.server.auth.service.testsources;

import java.security.PublicKey;
import java.util.Collections;
import java.util.List;
import org.sdase.commons.server.auth.key.JwksKeySource;
import org.sdase.commons.server.auth.key.KeyLoadFailedException;
import org.sdase.commons.server.auth.key.LoadedPublicKey;

public class JwksTestKeySource extends JwksKeySource {

  private final PublicKey publicKey;
  private final String requiredIssuer;
  private final String kid;
  private final String alg;

  public JwksTestKeySource(
      String jwksUri, PublicKey publicKey, String requiredIssuer, String kid, String alg) {
    super(jwksUri, null, requiredIssuer);
    this.publicKey = publicKey;
    this.requiredIssuer = requiredIssuer;
    this.kid = kid;
    this.alg = alg;
  }

  @Override
  public List<LoadedPublicKey> loadKeysFromSource() throws KeyLoadFailedException {
    return Collections.singletonList(
        new LoadedPublicKey(kid, publicKey, this, requiredIssuer, alg));
  }
}
