package org.sdase.commons.server.s3;

import jakarta.validation.constraints.NotEmpty;

/** Defines the configuration ot the {@link S3Bundle}. */
public class S3Configuration {

  /**
   * URL of an S3-compatible endpoint.
   *
   * <pre>
   * {@code http://servername:8080}
   * </pre>
   */
  @NotEmpty private String endpoint;

  /** Region where the S3 storage is located, can be left empty. Default is 'eu-central-1'. */
  private String region = "eu-central-1";

  /**
   * The access key to identify the accessor. If accessKey and secretKey are empty we will use the
   * <a
   * href="https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/auth/credentials/DefaultCredentialsProvider.html">default
   * credentials provider chain</a>.
   */
  private String accessKey;

  /**
   * The secret key of the accessor. If accessKey and secretKey are empty we will use the <a
   * href="https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/auth/credentials/DefaultCredentialsProvider.html">default
   * credentials provider chain</a>.
   */
  private String secretKey;

  /**
   * If true, the default credentials provider chain will be replaced with an anonymous credentials
   * provider. This is useful for testing against a local S3 server that does not require
   * authentication. Default is false.
   */
  private boolean useAnonymousLogin;

  /**
   * The signer type to use, overrides the default behavior, default is {@code AWSS3V4SignerType}.
   * This type is used to create a {@code software.amazon.awssdk.core.signer.Signer} instance. The
   * value should correspond to the simple class name of the signer.
   *
   * <p>Supported values are:
   *
   * <ul>
   *   <li>AWSS3V4SignerType (for backwards compatibility)
   *   <li>AwsS3V4SignerType
   *   <li>AwsS3V4Signer
   *   <li>--
   *   <li>Aws4SignerType
   *   <li>Aws4Signer
   *   <li>--
   *   <li>AwsCrtV4aSignerType
   *   <li>AwsCrtV4aSigner
   *   <li>--
   *   <li>AwsCrtS3V4aSignerType
   *   <li>AwsCrtS3V4aSigner </ul
   */
  private String signerOverride = "AWSS3V4SignerType";

  public String getEndpoint() {
    return endpoint;
  }

  public S3Configuration setEndpoint(String endpoint) {
    this.endpoint = endpoint;
    return this;
  }

  public String getRegion() {
    return region;
  }

  public S3Configuration setRegion(String region) {
    this.region = region;
    return this;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public S3Configuration setAccessKey(String accessKey) {
    this.accessKey = accessKey;
    return this;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public S3Configuration setSecretKey(String secretKey) {
    this.secretKey = secretKey;
    return this;
  }

  public boolean isUseAnonymousLogin() {
    return useAnonymousLogin;
  }

  public S3Configuration setUseAnonymousLogin(boolean useAnonymousLogin) {
    this.useAnonymousLogin = useAnonymousLogin;
    return this;
  }

  public String getSignerOverride() {
    return signerOverride;
  }

  public S3Configuration setSignerOverride(String signerOverride) {
    this.signerOverride = signerOverride;
    return this;
  }
}
