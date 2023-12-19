package org.sdase.commons.server.s3;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.core.Configuration;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

class S3BundleCredentialsTest {

  @SetSystemProperty(key = "aws.accessKeyId", value = "foo")
  @SetSystemProperty(key = "aws.secretAccessKey", value = "bar")
  @Test
  void shouldSupportCredentialsFromSystemProperties() {
    // given
    var s3Config =
        new S3Configuration().setAccessKey(null).setSecretKey(null).setUseAnonymousLogin(false);
    var s3Bundle = createS3BundleWithConfig(s3Config);

    // when
    var credentialsProvider = s3Bundle.createCredentialsProvider(s3Config);

    // then
    var awsCredentials = credentialsProvider.resolveCredentials();
    assertThat(awsCredentials.accessKeyId()).isEqualTo("foo");
    assertThat(awsCredentials.secretAccessKey()).isEqualTo("bar");
  }

  @Test
  void shouldSupportAnonymousCredentials() {
    // given
    var s3Config = new S3Configuration().setUseAnonymousLogin(true);
    var s3Bundle = createS3BundleWithConfig(s3Config);

    // when
    var credentialsProvider = s3Bundle.createCredentialsProvider(s3Config);

    // then
    var awsCredentials = credentialsProvider.resolveCredentials();
    assertThat(awsCredentials.accessKeyId()).isNull();
    assertThat(awsCredentials.secretAccessKey()).isNull();
  }

  private static S3Bundle<Configuration> createS3BundleWithConfig(S3Configuration s3Config) {
    return S3Bundle.builder().withConfigurationProvider(configuration -> s3Config).build();
  }
}
