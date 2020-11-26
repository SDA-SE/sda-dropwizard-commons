package org.sdase.commons.server.kms.aws.testing.model;

import com.amazonaws.services.kms.model.DecryptResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.ByteBuffer;

public class ExtendedDecryptResult extends DecryptResult {

  @Override
  @JsonProperty("KeyId")
  public String getKeyId() {
    return super.getKeyId();
  }

  @Override
  @JsonProperty("Plaintext")
  public ByteBuffer getPlaintext() {
    return super.getPlaintext();
  }

  @Override
  @JsonProperty("EncryptionAlgorithm")
  public String getEncryptionAlgorithm() {
    return super.getEncryptionAlgorithm();
  }
}
