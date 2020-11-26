package org.sdase.commons.server.kms.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.encryptionsdk.CryptoMaterialsManager;
import com.amazonaws.encryptionsdk.DefaultCryptoMaterialsManager;
import com.amazonaws.encryptionsdk.caching.CachingCryptoMaterialsManager;
import com.amazonaws.encryptionsdk.caching.LocalCryptoMaterialsCache;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.sdase.commons.server.kms.aws.config.KmsAwsCachingConfiguration;
import org.sdase.commons.server.kms.aws.config.KmsAwsConfiguration;
import org.sdase.commons.server.kms.aws.config.KmsAwsConfigurationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Bundle for AWS Client-Side Encryption. Keys are retrieved from AWS KMS */
public class KmsAwsBundle<C extends Configuration> implements ConfiguredBundle<C> {

  private static final Logger LOGGER = LoggerFactory.getLogger(KmsAwsBundle.class);

  private final KmsAwsConfigurationProvider<C> configurationProvider;

  private KmsAwsConfiguration kmsAwsConfiguration;
  private KmsMasterKeyProvider.Builder kmsMasterKeyProviderBuilder;

  protected KmsAwsBundle(KmsAwsConfigurationProvider<C> configurationProvider) {
    this.configurationProvider = configurationProvider;
  }

  public static InitialBuilder builder() {
    return new Builder<>();
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // nothing to initialize
  }

  public KmsAwsEncryptionService createKmsAwsEncryptionService(String keyArn) {

    if (kmsAwsConfiguration.isDisableAwsEncryption()) {
      LOGGER.warn("AWS KMS Encryption is DISABLED by configuration!");
      return KmsAwsEncryptionService.builder().withEncryptionDisabled().build();
    }

    KmsMasterKeyProvider masterKeyProvider = kmsMasterKeyProviderBuilder.buildStrict(keyArn);
    CryptoMaterialsManager cryptoMaterialsManager = initCryptoMaterialsManager(masterKeyProvider);
    return KmsAwsEncryptionService.builder()
        .withCryptoMaterialsManager(cryptoMaterialsManager)
        .build();
  }

  public KmsAwsDecryptionService createKmsAwsDecryptionService() {

    if (kmsAwsConfiguration.isDisableAwsEncryption()) {
      LOGGER.warn("AWS KMS Decryption is DISABLED by configuration!");
      return KmsAwsDecryptionService.builder().withDecryptionDisabled().build();
    }

    KmsMasterKeyProvider masterKeyProvider = kmsMasterKeyProviderBuilder.buildDiscovery();
    CryptoMaterialsManager cryptoMaterialsManager = initCryptoMaterialsManager(masterKeyProvider);
    return KmsAwsDecryptionService.builder()
        .withCryptoMaterialsManager(cryptoMaterialsManager)
        .build();
  }

  @Override
  public void run(C configuration, Environment environment) {
    this.kmsAwsConfiguration = configurationProvider.apply(configuration);
    this.kmsMasterKeyProviderBuilder = initKmsMasterKeyProviderBuilder();
  }

  private KmsMasterKeyProvider.Builder initKmsMasterKeyProviderBuilder() {
    KmsMasterKeyProvider.Builder masterKeyProviderBuilder;

    if (StringUtils.isNotBlank(kmsAwsConfiguration.getEndpointUrl())) {
      masterKeyProviderBuilder =
          KmsMasterKeyProvider.builder()
              .withCustomClientFactory(
                  regionName ->
                      AWSKMSClientBuilder.standard()
                          .withEndpointConfiguration(
                              new AwsClientBuilder.EndpointConfiguration(
                                  kmsAwsConfiguration.getEndpointUrl(),
                                  kmsAwsConfiguration.getRegion()))
                          .withCredentials(
                              new AWSStaticCredentialsProvider(
                                  new BasicAWSCredentials(
                                      kmsAwsConfiguration.getAccessKeyId(),
                                      kmsAwsConfiguration.getSecretAccessKey())))
                          .build());
      LOGGER.info("Using AWS KMS endpoint {}.", kmsAwsConfiguration.getEndpointUrl());
    } else {
      AWSCredentialsProvider credentialsProvider = getCredentialsProvider(kmsAwsConfiguration);
      masterKeyProviderBuilder =
          KmsMasterKeyProvider.builder()
              .withClientBuilder(
                  AWSKMSClientBuilder.standard()
                      .withCredentials(credentialsProvider)
                      .withRegion(kmsAwsConfiguration.getRegion()));
      LOGGER.info("Using AWS KMS in region {}.", kmsAwsConfiguration.getRegion());
    }
    return masterKeyProviderBuilder;
  }

  private CryptoMaterialsManager initCryptoMaterialsManager(
      KmsMasterKeyProvider masterKeyProvider) {

    CryptoMaterialsManager cryptoMaterialsManager;

    KmsAwsCachingConfiguration cachingConfig = kmsAwsConfiguration.getKeyCaching();

    if (cachingConfig != null && cachingConfig.isEnabled()) {

      cryptoMaterialsManager =
          CachingCryptoMaterialsManager.newBuilder()
              .withMasterKeyProvider(masterKeyProvider)
              .withCache(new LocalCryptoMaterialsCache(cachingConfig.getMaxCacheSize()))
              .withMaxAge(cachingConfig.getKeyMaxLifetimeInSeconds(), TimeUnit.SECONDS)
              .withMessageUseLimit(cachingConfig.getMaxMessagesPerKey())
              .build();

      LOGGER.info("AWS KMS Decryption Service configured with key-caching enabled.");
    } else {
      cryptoMaterialsManager = new DefaultCryptoMaterialsManager(masterKeyProvider);
    }

    return cryptoMaterialsManager;
  }

  private AWSCredentialsProvider getCredentialsProvider(KmsAwsConfiguration kmsAwsConfiguration) {

    String regionStr = kmsAwsConfiguration.getRegion();
    Regions region = Regions.fromName(regionStr);
    String roleArn = kmsAwsConfiguration.getRoleArn();
    String roleSessionName = kmsAwsConfiguration.getRoleSessionNamePrefix() + UUID.randomUUID();
    String accessKeyId = kmsAwsConfiguration.getAccessKeyId();
    String secretAccessKey = kmsAwsConfiguration.getSecretAccessKey();
    BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
    AWSStaticCredentialsProvider awsCredentialsProvider =
        new AWSStaticCredentialsProvider(awsCredentials);

    LOGGER.debug("Creating AWSSecurityTokenServiceClient in region '{}'...", region);

    AWSSecurityTokenService awsSecurityTokenService =
        AWSSecurityTokenServiceClient.builder()
            .withCredentials(awsCredentialsProvider)
            .withRegion(region)
            .build();

    LOGGER.info(
        "Creating AWSCredentialsProvider with roleArn '{}' and roleSessionName '{}'...",
        roleArn,
        roleSessionName);

    return new STSAssumeRoleSessionCredentialsProvider.Builder(roleArn, roleSessionName)
        .withStsClient(awsSecurityTokenService)
        .build();
  }

  public interface InitialBuilder {
    <C extends Configuration> FinalBuilder<C> withConfigurationProvider(
        @NotNull KmsAwsConfigurationProvider<C> configurationProvider);
  }

  public interface FinalBuilder<C extends Configuration> {
    KmsAwsBundle<C> build();
  }

  public static class Builder<T extends Configuration> implements InitialBuilder, FinalBuilder<T> {

    private final KmsAwsConfigurationProvider<T> configProvider;

    protected Builder() {
      configProvider = null;
    }

    protected Builder(KmsAwsConfigurationProvider<T> configProvider) {
      this.configProvider = configProvider;
    }

    @Override
    public <C extends Configuration> FinalBuilder<C> withConfigurationProvider(
        KmsAwsConfigurationProvider<C> configurationProvider) {
      return new Builder<>(configurationProvider);
    }

    @Override
    public KmsAwsBundle<T> build() {
      return new KmsAwsBundle<>(configProvider);
    }
  }
}
