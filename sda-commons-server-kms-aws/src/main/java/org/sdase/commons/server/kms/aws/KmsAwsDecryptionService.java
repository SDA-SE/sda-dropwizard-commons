package org.sdase.commons.server.kms.aws;

import com.amazonaws.encryptionsdk.*;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KmsAwsDecryptionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(KmsAwsDecryptionService.class);

  private final AwsCrypto awsCrypto =
      AwsCrypto.builder()
          .withCommitmentPolicy(CommitmentPolicy.RequireEncryptRequireDecrypt)
          .build();

  private final boolean disableCrypto;

  private final CryptoMaterialsManager cryptoMaterialsManagerDiscoveryMode;

  protected KmsAwsDecryptionService(
      CryptoMaterialsManager cryptoMaterialsManagerDiscoveryMode, boolean disableCrypto) {
    this.cryptoMaterialsManagerDiscoveryMode = cryptoMaterialsManagerDiscoveryMode;
    this.disableCrypto = disableCrypto;
  }

  /**
   * Decrypts the given ciphertext and validates the included encryption context by verify that
   * least all key-value paris of the expectedEncryptionContext are included in the
   * encryptionContext provided with the decrypted ciphertext.
   *
   * @param ciphertext content to be decrypted
   * @param expectedEncryptionContext minimum set of key/value pairs expected to be included in the
   *     decrypted ciphertext
   * @return decrypted ciphertext
   * @throws InvalidEncryptionContextException in case the key-value pairs of the
   *     expectedEncryptionContext are not included in the encryption context of the decrypted
   *     payload.
   * @see AwsCrypto#decryptData(MasterKeyProvider, byte[])
   * @see AwsCrypto#decryptData(CryptoMaterialsManager, byte[])
   */
  public byte[] decrypt(byte[] ciphertext, Map<String, String> expectedEncryptionContext)
      throws InvalidEncryptionContextException {
    if (disableCrypto) {
      LOGGER.debug("Decryption is disabled");
      return ciphertext;
    }

    CryptoResult<byte[], ?> cryptoResult =
        awsCrypto.decryptData(cryptoMaterialsManagerDiscoveryMode, ciphertext);

    validate(cryptoResult, expectedEncryptionContext);
    return cryptoResult.getResult();
  }

  /**
   * Verify that the kms context in the result contains the kms context supplied to the
   * encryptString method. Because the SDK can add values to the kms context, don't require that the
   * entire context matches.
   */
  private void validate(
      CryptoResult<byte[], ?> cryptoResult, Map<String, String> expectedEncryptionContext)
      throws InvalidEncryptionContextException {
    // Since we are always running in discovery mode we do not supply a key id here,
    // so we do not need to validate the keyArn against the MasterKeyIds

    if (expectedEncryptionContext == null
        || expectedEncryptionContext.size() < 1
        || !expectedEncryptionContext.entrySet().stream()
            .allMatch(
                e -> e.getValue().equals(cryptoResult.getEncryptionContext().get(e.getKey())))) {
      throw new InvalidEncryptionContextException();
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private boolean disableDecryption;
    private CryptoMaterialsManager cryptoMaterialsManager;

    private Builder() {
      //
    }

    public Builder withDecryptionDisabled() {
      this.disableDecryption = true;
      return this;
    }

    public Builder withCryptoMaterialsManager(CryptoMaterialsManager cryptoMaterialsManager) {
      this.cryptoMaterialsManager = cryptoMaterialsManager;
      this.disableDecryption = false;
      return this;
    }

    public KmsAwsDecryptionService build() {
      return new KmsAwsDecryptionService(cryptoMaterialsManager, disableDecryption);
    }
  }
}
