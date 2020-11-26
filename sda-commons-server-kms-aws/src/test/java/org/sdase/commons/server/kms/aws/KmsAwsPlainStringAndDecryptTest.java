package org.sdase.commons.server.kms.aws;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;
import org.junit.Test;

public class KmsAwsPlainStringAndDecryptTest {

  @Test
  public void returnIfCryptoIsDisabledEncryption() {
    String encryptionInput = "encryptionInput";

    KmsAwsEncryptionService encryptionService =
        KmsAwsEncryptionService.builder().withEncryptionDisabled().build();

    byte[] encrypted = encryptionService.encrypt(encryptionInput.getBytes(), null);
    assertThat(encryptionInput).isEqualTo(new String(encrypted));
  }

  @Test
  public void returnIfCryptoIsDisabledDecryption() throws InvalidEncryptionContextException {
    String decryptionInput = "decryptionInput";
    KmsAwsDecryptionService decryptionService =
        KmsAwsDecryptionService.builder().withDecryptionDisabled().build();

    byte[] decrypted = decryptionService.decrypt(decryptionInput.getBytes(), null);
    assertThat(decryptionInput).isEqualTo(new String(decrypted));
  }

  @Test
  public void encryptAndDecryptWithCachingTest() throws InvalidEncryptionContextException {
    String encryptionInput = "encryptionInput";
    KmsAwsEncryptionServiceMock kmsAwsEncryptionServiceMock = new KmsAwsEncryptionServiceMock(true);
    KmsAwsEncryptionService kmsAwsEncryptionService =
        kmsAwsEncryptionServiceMock.getKmsAwsEncryptionService();

    Map<String, String> context = Collections.singletonMap("key", "value");

    byte[] encrypt = kmsAwsEncryptionService.encrypt(encryptionInput.getBytes(), context);

    KmsAwsDecryptionService kmsAwsDecryptionService =
        new KmsAwsDecryptionServiceMock(true, kmsAwsEncryptionServiceMock.getMockKmsClient())
            .getKmsAwsDecryptionService();

    byte[] decrypt = kmsAwsDecryptionService.decrypt(encrypt, context);

    assertThat(encryptionInput).isEqualTo(new String(decrypt));
  }

  @Test
  public void encryptAndDecryptWithoutCachingTest() throws InvalidEncryptionContextException {
    String encryptionInput = "encryptionInput";
    KmsAwsEncryptionServiceMock kmsAwsEncryptionServiceMock =
        new KmsAwsEncryptionServiceMock(false);
    KmsAwsEncryptionService kmsAwsEncryptionService =
        kmsAwsEncryptionServiceMock.getKmsAwsEncryptionService();

    Map<String, String> context = Collections.singletonMap("key", "value");

    byte[] encrypt = kmsAwsEncryptionService.encrypt(encryptionInput.getBytes(), context);

    KmsAwsDecryptionService kmsAwsDecryptionService =
        new KmsAwsDecryptionServiceMock(false, kmsAwsEncryptionServiceMock.getMockKmsClient())
            .getKmsAwsDecryptionService();

    byte[] decrypt = kmsAwsDecryptionService.decrypt(encrypt, context);

    assertThat(encryptionInput).isEqualTo(new String(decrypt));
  }

  @Test(expected = InvalidEncryptionContextException.class)
  public void decryptShouldFailWithWrongValueEncryptionContextExceptionTest()
      throws InvalidEncryptionContextException {

    String encryptionInput = "encryptionInput";
    KmsAwsEncryptionServiceMock kmsAwsEncryptionServiceMock =
        new KmsAwsEncryptionServiceMock(false);
    KmsAwsEncryptionService kmsAwsEncryptionService =
        kmsAwsEncryptionServiceMock.getKmsAwsEncryptionService();

    Map<String, String> context = Collections.singletonMap("key", "value");

    byte[] encrypt = kmsAwsEncryptionService.encrypt(encryptionInput.getBytes(), context);

    KmsAwsDecryptionService kmsAwsDecryptionService =
        new KmsAwsDecryptionServiceMock(false, kmsAwsEncryptionServiceMock.getMockKmsClient())
            .getKmsAwsDecryptionService();

    Map<String, String> decryptContext = Collections.singletonMap("key", "wrongValue");
    kmsAwsDecryptionService.decrypt(encrypt, decryptContext);
  }

  @Test(expected = InvalidEncryptionContextException.class)
  public void decryptShouldFailWithWrongKeyEncryptionContextExceptionTest()
      throws InvalidEncryptionContextException {
    String encryptionInput = "encryptionInput";
    KmsAwsEncryptionServiceMock kmsAwsEncryptionServiceMock =
        new KmsAwsEncryptionServiceMock(false);
    KmsAwsEncryptionService kmsAwsEncryptionService =
        kmsAwsEncryptionServiceMock.getKmsAwsEncryptionService();

    Map<String, String> context = Collections.singletonMap("key", "value");

    byte[] encrypt = kmsAwsEncryptionService.encrypt(encryptionInput.getBytes(), context);

    KmsAwsDecryptionService kmsAwsDecryptionService =
        new KmsAwsDecryptionServiceMock(false, kmsAwsEncryptionServiceMock.getMockKmsClient())
            .getKmsAwsDecryptionService();

    Map<String, String> decryptContext = Collections.singletonMap("key1", "value");
    kmsAwsDecryptionService.decrypt(encrypt, decryptContext);
  }

  @Test(expected = InvalidEncryptionContextException.class)
  public void decryptShouldSucceedWithNullEncryptionContextExceptionTest()
      throws InvalidEncryptionContextException {
    String encryptionInput = "encryptionInput";
    KmsAwsEncryptionServiceMock kmsAwsEncryptionServiceMock =
        new KmsAwsEncryptionServiceMock(false);
    KmsAwsEncryptionService kmsAwsEncryptionService =
        kmsAwsEncryptionServiceMock.getKmsAwsEncryptionService();

    Map<String, String> context = Collections.singletonMap("key", "value");

    byte[] encrypt = kmsAwsEncryptionService.encrypt(encryptionInput.getBytes(), context);

    KmsAwsDecryptionService kmsAwsDecryptionService =
        new KmsAwsDecryptionServiceMock(false, kmsAwsEncryptionServiceMock.getMockKmsClient())
            .getKmsAwsDecryptionService();

    kmsAwsDecryptionService.decrypt(encrypt, null);
  }
}
