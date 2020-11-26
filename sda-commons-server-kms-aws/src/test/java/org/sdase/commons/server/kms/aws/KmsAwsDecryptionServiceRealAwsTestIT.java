package org.sdase.commons.server.kms.aws;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

import com.amazonaws.encryptionsdk.exception.BadCiphertextException;
import com.amazonaws.services.securitytoken.model.AWSSecurityTokenServiceException;
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

/**
 * As we cannot test against a real AWS KMS we will at least test that the correct exceptions are
 * thrown for encrypt/decrypt showing that the initialization was done. Better than nothing. ;)
 */
public class KmsAwsDecryptionServiceRealAwsTestIT {

  private static final DropwizardAppRule<AwsEncryptionTestConfiguration> DROPWIZARD_APP_RULE =
      new DropwizardAppRule<>(
          AwsEncryptionTestApplication.class,
          resourceFilePath("test-config-default.yml"),
          config("kmsAws.keyCaching.enabled", "false"));

  private KmsAwsBundle<AwsEncryptionTestConfiguration> kmsAwsBundle;

  @ClassRule public static final RuleChain CHAIN = RuleChain.outerRule(DROPWIZARD_APP_RULE);

  @Before
  public void before() {
    AwsEncryptionTestApplication app = DROPWIZARD_APP_RULE.getApplication();
    kmsAwsBundle = app.AwsEncryptionBundle();
  }

  @Test(expected = AWSSecurityTokenServiceException.class)
  public void encryptShouldThrowAWSSecurityTokenServiceException() {
    byte[] plaintext = UUID.randomUUID().toString().getBytes();
    Map<String, String> encryptionContext = new HashMap<>();
    encryptionContext.put("key", "value");
    kmsAwsBundle
        .createKmsAwsEncryptionService("arn:aws:kms:eu-central-1:test-account:alias/testing")
        .encrypt(plaintext, encryptionContext);
  }

  @Test(expected = BadCiphertextException.class)
  public void decryptShouldThrowAWSSecurityTokenServiceException()
      throws InvalidEncryptionContextException {
    byte[] plaintext = UUID.randomUUID().toString().getBytes();

    kmsAwsBundle.createKmsAwsDecryptionService().decrypt(plaintext, new HashMap<>());
  }
}
