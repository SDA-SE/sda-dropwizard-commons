package org.sdase.commons.server.kms.aws.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class KmsAwsConfigurationTest {
  KmsAwsConfiguration classUnderTest = new KmsAwsConfiguration();

  @Test
  public void encryptionDisabledTest() {
    classUnderTest.setDisableAwsEncryption(true);

    assertThat(classUnderTest.isValid()).isTrue();
  }

  @Test
  public void encryptionEnabledKeyArnBlankTest() {
    classUnderTest.setDisableAwsEncryption(false);

    assertThat(classUnderTest.isValid()).isFalse();
  }

  @Test
  public void encryptionEnabledEndpointUrlNotBlankTest() {
    classUnderTest.setDisableAwsEncryption(false);
    classUnderTest.setEndpointUrl("localhost:3423");

    assertThat(classUnderTest.isValid()).isTrue();
  }

  @Test
  public void encryptionEnabledEndpointUrlBlankTest() {
    classUnderTest.setDisableAwsEncryption(false);
    classUnderTest.setEndpointUrl("");
    classUnderTest.setAccessKeyId("accessKey");
    classUnderTest.setSecretAccessKey("secretKey");
    classUnderTest.setRoleArn("roleArn");
    classUnderTest.setRegion("region");
    classUnderTest.setRoleSessionNamePrefix("roleSession");

    assertThat(classUnderTest.isValid()).isTrue();
  }

  @Test
  public void encryptionEnabledEndpointUrlBlankAndRegionBlankTest() {
    classUnderTest.setDisableAwsEncryption(false);
    classUnderTest.setEndpointUrl("");
    classUnderTest.setAccessKeyId("accessKey");
    classUnderTest.setSecretAccessKey("secretKey");
    classUnderTest.setRoleArn("roleArn");
    classUnderTest.setRegion("");
    classUnderTest.setRoleSessionNamePrefix("roleSession");

    assertThat(classUnderTest.isValid()).isFalse();
  }
}
