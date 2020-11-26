package org.sdase.commons.server.kms.aws.dropwizard;

import io.dropwizard.Configuration;
import javax.validation.Valid;
import org.sdase.commons.server.kms.aws.config.KmsAwsConfiguration;

public class AwsEncryptionTestConfiguration extends Configuration {

  public AwsEncryptionTestConfiguration() {}

  @Valid private KmsAwsConfiguration kmsAws = new KmsAwsConfiguration();

  public KmsAwsConfiguration getKmsAws() {
    return kmsAws;
  }
}
