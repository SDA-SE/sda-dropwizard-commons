package org.sdase.commons.server.kms.aws.testing.model;

import com.amazonaws.services.kms.model.GenerateDataKeyResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.ByteBuffer;

public class ExtendedGenerateDataKeyResult extends GenerateDataKeyResult {

  @Override
  @JsonProperty("CiphertextBlob")
  public ByteBuffer getCiphertextBlob() {
    return super.getCiphertextBlob();
  }

  @Override
  @JsonProperty("KeyId")
  public String getKeyId() {
    return super.getKeyId();
  }

  @Override
  @JsonProperty("Plaintext")
  public java.nio.ByteBuffer getPlaintext() {
    return super.getPlaintext();
  }
}
