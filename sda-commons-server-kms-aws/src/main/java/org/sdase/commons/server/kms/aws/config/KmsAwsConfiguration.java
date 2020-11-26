package org.sdase.commons.server.kms.aws.config;

import io.dropwizard.Configuration;
import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import org.apache.commons.lang3.StringUtils;

public class KmsAwsConfiguration extends Configuration {
  /** Region where the referenced AWS KMS is located (e.g. <code>eu-central-1</code>) */
  private String region;

  /** Access Key Id of the user able to retrieve the key referenced by <code>keyArn</code> */
  private String accessKeyId;

  /** Secret access key of the user able to retrieve the key referenced by <code>keyArn</code> */
  private String secretAccessKey;

  /**
   * ARN of the role which the user belongs to holding the access key referenced by <code>
   * accessKeyId</code>
   */
  private String roleArn;

  /**
   * Prefix for the role session name. This value will be appended by a random UUID. It is used to
   * identify an assumed role session (e.g. in CloudTrail logs).
   */
  private String roleSessionNamePrefix;

  /**
   * For testing purposes only kms/decryption can be disabled completely. Do not use in Production!
   */
  private boolean disableAwsEncryption = false;

  /**
   * Pointing to a service or mock (e.g. Wiremock) acting like a real AWS KMS, but used for testing
   * purposes when no real AWS KMS is available
   */
  private String endpointUrl;

  @Valid private KmsAwsCachingConfiguration keyCaching;

  public String getRegion() {
    return region;
  }

  public KmsAwsConfiguration setRegion(String region) {
    this.region = region;
    return this;
  }

  public String getAccessKeyId() {
    return accessKeyId;
  }

  public KmsAwsConfiguration setAccessKeyId(String accessKeyId) {
    this.accessKeyId = accessKeyId;
    return this;
  }

  public String getSecretAccessKey() {
    return secretAccessKey;
  }

  public KmsAwsConfiguration setSecretAccessKey(String secretAccessKey) {
    this.secretAccessKey = secretAccessKey;
    return this;
  }

  public String getRoleArn() {
    return roleArn;
  }

  public KmsAwsConfiguration setRoleArn(String roleArn) {
    this.roleArn = roleArn;
    return this;
  }

  public String getEndpointUrl() {
    return endpointUrl;
  }

  public KmsAwsConfiguration setEndpointUrl(String endpointUrl) {
    this.endpointUrl = endpointUrl;
    return this;
  }

  public boolean isDisableAwsEncryption() {
    return disableAwsEncryption;
  }

  public KmsAwsConfiguration setDisableAwsEncryption(boolean disableAwsEncryption) {
    this.disableAwsEncryption = disableAwsEncryption;
    return this;
  }

  public String getRoleSessionNamePrefix() {
    return roleSessionNamePrefix;
  }

  public KmsAwsConfiguration setRoleSessionNamePrefix(String roleSessionNamePrefix) {
    this.roleSessionNamePrefix = roleSessionNamePrefix;
    return this;
  }

  public KmsAwsCachingConfiguration getKeyCaching() {
    return keyCaching;
  }

  public KmsAwsConfiguration setKeyCaching(KmsAwsCachingConfiguration keyCaching) {
    this.keyCaching = keyCaching;
    return this;
  }

  @AssertTrue
  public boolean isValid() {
    if (disableAwsEncryption) {
      return true;
    }

    if (StringUtils.isBlank(endpointUrl)) {
      return StringUtils.isNotBlank(accessKeyId)
          && StringUtils.isNotBlank(secretAccessKey)
          && StringUtils.isNotBlank(roleArn)
          && StringUtils.isNotBlank(region)
          && StringUtils.isNotBlank(roleSessionNamePrefix);
    }
    return true;
  }
}
