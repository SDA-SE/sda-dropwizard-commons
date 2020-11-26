package org.sdase.commons.server.kms.aws;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.kms.aws.dropwizard.AwsEncryptionTestApplication;
import org.sdase.commons.server.kms.aws.dropwizard.AwsEncryptionTestConfiguration;

public class KmsAwsDecryptionServiceDisabledCryptoTestIT {

  @ClassRule
  public static final DropwizardAppRule<AwsEncryptionTestConfiguration> DROPWIZARD_APP_RULE =
      new DropwizardAppRule<>(
          AwsEncryptionTestApplication.class,
          resourceFilePath("test-config-default.yml"),
          config("kmsAws.disableAwsEncryption", "true"));

  private KmsAwsBundle<AwsEncryptionTestConfiguration> kmsAwsBundle;

  @Before
  public void before() {
    AwsEncryptionTestApplication app = DROPWIZARD_APP_RULE.getApplication();
    kmsAwsBundle = app.AwsEncryptionBundle();
  }

  @Test
  public void shouldEncryptAndDecryptSuccessfully() throws InvalidEncryptionContextException {
    byte[] plaintext = UUID.randomUUID().toString().getBytes();
    Map<String, String> encryptionContext = new HashMap<>();
    encryptionContext.put("key", "value");
    byte[] encryptedContent =
        kmsAwsBundle
            .createKmsAwsEncryptionService("arn:aws:kms:eu-central-1:test-account:alias/testing")
            .encrypt(plaintext, encryptionContext);

    assertThat(encryptedContent).isNotEmpty();

    assertThat(
            kmsAwsBundle
                .createKmsAwsDecryptionService()
                .decrypt(encryptedContent, encryptionContext))
        .isEqualTo(plaintext);
  }

  @Test
  public void shouldEncryptAndDecryptSuccessfullyWithoutEncryptionContext()
      throws InvalidEncryptionContextException {
    byte[] plaintext = UUID.randomUUID().toString().getBytes();

    byte[] encryptedContent =
        kmsAwsBundle
            .createKmsAwsEncryptionService("arn:aws:kms:eu-central-1:test-account:alias/testing")
            .encrypt(plaintext, new HashMap<>());

    assertThat(encryptedContent).isNotEmpty();

    assertThat(kmsAwsBundle.createKmsAwsDecryptionService().decrypt(encryptedContent, null))
        .isEqualTo(plaintext);
  }
}
