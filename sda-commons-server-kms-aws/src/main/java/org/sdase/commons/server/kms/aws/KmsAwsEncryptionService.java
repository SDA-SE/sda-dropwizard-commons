package org.sdase.commons.server.kms.aws;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CommitmentPolicy;
import com.amazonaws.encryptionsdk.CryptoMaterialsManager;
import com.amazonaws.encryptionsdk.MasterKeyProvider;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KmsAwsEncryptionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(KmsAwsEncryptionService.class);

  private final AwsCrypto awsCrypto =
      AwsCrypto.builder()
          .withCommitmentPolicy(CommitmentPolicy.RequireEncryptRequireDecrypt)
          .build();

  private final boolean disableCrypto;

  private final CryptoMaterialsManager cryptoMaterialsManagerStrictMode;

  protected KmsAwsEncryptionService(
      CryptoMaterialsManager cryptoMaterialsManagerStrictMode, boolean disableCrypto) {
    this.cryptoMaterialsManagerStrictMode = cryptoMaterialsManagerStrictMode;
    this.disableCrypto = disableCrypto;
  }

  /**
   * Returns an encrypted form of plaintext including the encrypted data key and the given
   * encryptionContext.
   *
   * @param plaintext content to be encrypted
   * @param encryptionContext key-value pairs included in the encrypted ciphertext
   * @return encrypted ciphertext
   * @see AwsCrypto#encryptData(MasterKeyProvider, byte[], Map)
   * @see AwsCrypto#encryptData(CryptoMaterialsManager, byte[], Map)
   */
  public byte[] encrypt(byte[] plaintext, Map<String, String> encryptionContext) {
    if (disableCrypto) {
      LOGGER.debug("Decryption is disabled");
      return plaintext;
    }

    return awsCrypto
        .encryptData(cryptoMaterialsManagerStrictMode, plaintext, encryptionContext)
        .getResult();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private boolean disableEncryption;
    private CryptoMaterialsManager cryptoMaterialsManager;

    private Builder() {
      //
    }

    public Builder withEncryptionDisabled() {
      this.disableEncryption = true;
      return this;
    }

    public Builder withCryptoMaterialsManager(CryptoMaterialsManager cryptoMaterialsManager) {
      this.cryptoMaterialsManager = cryptoMaterialsManager;
      this.disableEncryption = false;
      return this;
    }

    public KmsAwsEncryptionService build() {
      return new KmsAwsEncryptionService(cryptoMaterialsManager, disableEncryption);
    }
  }
}
