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
import org.junit.rules.RuleChain;
import org.sdase.commons.server.kms.aws.dropwizard.AwsEncryptionTestApplication;
import org.sdase.commons.server.kms.aws.dropwizard.AwsEncryptionTestConfiguration;
import org.sdase.commons.server.kms.aws.testing.KmsAwsRule;

public class KmsAwsDecryptionServiceEnabledCachingTestIT {

  private static final KmsAwsRule KMS_RULE = new KmsAwsRule();

  private static final DropwizardAppRule<AwsEncryptionTestConfiguration> DROPWIZARD_APP_RULE =
      new DropwizardAppRule<>(
          AwsEncryptionTestApplication.class,
          resourceFilePath("test-config-default.yml"),
          config("kmsAws.endpointUrl", KMS_RULE::getEndpointUrl),
          config("kmsAws.keyCaching.enabled", "true"),
          config("kmsAws.keyCaching.maxCacheSize", "10"),
          config("kmsAws.keyCaching.keyMaxLifetimeInSeconds", "10"),
          config("kmsAws.keyCaching.maxMessagesPerKey", "10"));

  private KmsAwsBundle<AwsEncryptionTestConfiguration> kmsAwsBundle;

  @ClassRule
  public static final RuleChain CHAIN = RuleChain.outerRule(KMS_RULE).around(DROPWIZARD_APP_RULE);

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

  @Test(expected = InvalidEncryptionContextException.class)
  public void shouldEncryptAndDecryptSuccessfullyWithoutEncryptionContext()
      throws InvalidEncryptionContextException {
    byte[] plaintext = UUID.randomUUID().toString().getBytes();

    byte[] encryptedContent =
        kmsAwsBundle
            .createKmsAwsEncryptionService("arn:aws:kms:eu-central-1:test-account:alias/testing")
            .encrypt(plaintext, new HashMap<>());

    assertThat(encryptedContent).isNotEmpty();

    kmsAwsBundle.createKmsAwsDecryptionService().decrypt(encryptedContent, null);
  }
}
