package org.sdase.commons.server.s3;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.sdase.commons.server.dropwizard.lifecycle.ManagedShutdownListener.onShutdown;
import static org.sdase.commons.server.s3.health.S3HealthCheckType.EXTERNAL;
import static org.sdase.commons.server.s3.health.S3HealthCheckType.INTERNAL;
import static org.sdase.commons.server.s3.health.S3HealthCheckType.NONE;

import io.dropwizard.core.Configuration;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.awssdk.v2_2.AwsSdkTelemetry;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.sdase.commons.server.s3.health.ExternalS3HealthCheck;
import org.sdase.commons.server.s3.health.S3HealthCheck;
import org.sdase.commons.server.s3.health.S3HealthCheckType;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.auth.signer.SignerLoader;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

public class S3Bundle<C extends Configuration> implements ConfiguredBundle<C> {

  public static final String S3_HEALTH_CHECK_NAME = "s3Connection";
  public static final String S3_EXTERNAL_HEALTH_CHECK_NAME = "s3ConnectionExternal";

  private final S3ConfigurationProvider<C> configurationProvider;
  private final OpenTelemetry openTelemetry;
  private final S3HealthCheckType s3HealthCheckType;
  private final Iterable<BucketNameProvider<C>> bucketNameProviders;
  private S3Configuration s3Configuration;
  private S3Client s3Client;

  private S3Bundle(
      S3ConfigurationProvider<C> configurationProvider,
      OpenTelemetry openTelemetry,
      S3HealthCheckType s3HealthCheckType,
      Iterable<BucketNameProvider<C>> bucketNameProviders) {
    this.configurationProvider = configurationProvider;
    this.openTelemetry = openTelemetry;
    this.s3HealthCheckType = s3HealthCheckType;
    this.bucketNameProviders = bucketNameProviders;
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // nothing to initialize
  }

  @Override
  public void run(C configuration, Environment environment) {
    this.s3Configuration = configurationProvider.apply(configuration);

    this.s3Client = newClient();

    environment.lifecycle().manage(onShutdown(s3Client::close));

    if (isHealthCheckEnabled()) {
      Set<String> bucketNames =
          StreamSupport.stream(bucketNameProviders.spliterator(), false)
              .map(provider -> provider.apply(configuration))
              .collect(Collectors.toSet());

      switch (s3HealthCheckType) {
        case INTERNAL:
          environment
              .healthChecks()
              .register(S3_HEALTH_CHECK_NAME, new S3HealthCheck(s3Client, bucketNames));
          break;
        case EXTERNAL:
          environment
              .healthChecks()
              .register(
                  S3_EXTERNAL_HEALTH_CHECK_NAME, new ExternalS3HealthCheck(s3Client, bucketNames));
          break;
        case NONE:
        default:
          break;
      }
    }
  }

  private S3Client newClient() {
    return S3Client.builder()
        .region(getRegion())
        .endpointOverride(URI.create(s3Configuration.getEndpoint()))
        .serviceConfiguration(
            software.amazon.awssdk.services.s3.S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build())
        .overrideConfiguration(
            ClientOverrideConfiguration.builder()
                .executionInterceptors(List.of(createTracingExecutingInterceptor()))
                .putAdvancedOption(SdkAdvancedClientOption.SIGNER, createSigner(s3Configuration))
                .build())
        .httpClient(UrlConnectionHttpClient.builder().build())
        .credentialsProvider(createCredentialsProvider(s3Configuration))
        .build();
  }

  public S3Presigner newPresigner() {
    return S3Presigner.builder()
        .s3Client(getClient())
        .region(getRegion())
        .credentialsProvider(createCredentialsProvider(s3Configuration))
        .build();
  }

  /**
   * @return the region to use for the S3 client, default is 'eu-central-1'
   */
  private Region getRegion() {
    String region = s3Configuration.getRegion();
    if (region != null && !region.isBlank()) {
      return Region.of(region);
    }

    // default
    return Region.EU_CENTRAL_1;
  }

  private ExecutionInterceptor createTracingExecutingInterceptor() {
    // Initialize a telemetry instance if not set.
    OpenTelemetry currentTelemetryInstance =
        this.openTelemetry == null ? GlobalOpenTelemetry.get() : this.openTelemetry;
    return AwsSdkTelemetry.builder(currentTelemetryInstance)
        .setCaptureExperimentalSpanAttributes(true)
        .build()
        .createExecutionInterceptor();
  }

  private Signer createSigner(S3Configuration s3Configuration) {
    String signerOverride = s3Configuration.getSignerOverride();
    if (signerOverride == null || signerOverride.isBlank()) {
      return AwsS3V4Signer.create();
    }
    // I don't know how to do that more elegantly
    return switch (signerOverride) {
      case "AWSS3V4SignerType", "AwsS3V4SignerType", "AwsS3V4Signer" -> AwsS3V4Signer.create();
      case "Aws4SignerType", "Aws4Signer" -> Aws4Signer.create();
      case "AwsCrtV4aSignerType", "AwsCrtV4aSigner" -> SignerLoader.getSigV4aSigner();
      case "AwsCrtS3V4aSignerType", "AwsCrtS3V4aSigner" -> SignerLoader.getS3SigV4aSigner();
      default -> throw new IllegalArgumentException("Unknown signer override: " + signerOverride);
    };
  }

  /**
   * Using basic credentials. If no credentials are provided, anonymous credentials are used.
   *
   * @return credentials provider
   */
  AwsCredentialsProvider createCredentialsProvider(S3Configuration config) {
    if (config.isUseAnonymousLogin()) {
      return AnonymousCredentialsProvider.create();
    }
    var accessKey = config.getAccessKey();
    var secretKey = config.getSecretKey();
    if (!isBlank(accessKey) && !isBlank(secretKey)) {
      return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }
    return DefaultCredentialsProvider.create();
  }

  private boolean isHealthCheckEnabled() {
    return !NONE.equals(this.s3HealthCheckType);
  }

  public S3Client getClient() {
    if (s3Client == null) {
      throw new IllegalStateException("S3 client accessed to early, can't be accessed before run.");
    }

    return s3Client;
  }

  //
  // Builder
  //
  public static InitialBuilder builder() {
    return new Builder<>();
  }

  public interface InitialBuilder {

    /**
     * @param configurationProvider the method reference that provides the @{@link S3Configuration}
     *     from the applications configurations class
     * @param <C> the type of the applications configuration class
     * @return the same builder
     */
    <C extends Configuration> S3HealthCheckBuilder<C> withConfigurationProvider(
        @NotNull S3ConfigurationProvider<C> configurationProvider);
  }

  public interface S3HealthCheckBuilder<C extends Configuration> extends FinalBuilder<C> {

    /**
     * Adds an internal health check for an S3 connection against one or more buckets.
     *
     * @param bucketNameProviders bucket name providers
     * @return the builder instance
     */
    FinalBuilder<C> withHealthCheck(Iterable<BucketNameProvider<C>> bucketNameProviders);

    /**
     * Adds an external health check for an S3 connection against one or more buckets.
     *
     * @param bucketNameProviders bucket name providers
     * @return the builder instance
     */
    FinalBuilder<C> withExternalHealthCheck(Iterable<BucketNameProvider<C>> bucketNameProviders);

    /**
     * Adds an internal health check for an S3 connection against a single bucket.
     *
     * @param bucketName the bucket name
     * @return the builder instance
     */
    FinalBuilder<C> withHealthCheck(String bucketName);

    /**
     * Adds an external health check for an S3 connection against a single bucket.
     *
     * @param bucketName the bucket name
     * @return the builder instance
     */
    FinalBuilder<C> withExternalHealthCheck(String bucketName);
  }

  public interface FinalBuilder<C extends Configuration> {

    /**
     * Specifies a custom telemetry instance to use. If no instance is specified, the {@link
     * GlobalOpenTelemetry} is used.
     *
     * @param openTelemetry The telemetry instance to use
     * @return the same builder
     */
    FinalBuilder<C> withOpenTelemetry(OpenTelemetry openTelemetry);

    /**
     * Builds the S3 bundle
     *
     * @return S3 bundle
     */
    S3Bundle<C> build();
  }

  public static class Builder<T extends Configuration>
      implements InitialBuilder, FinalBuilder<T>, S3HealthCheckBuilder<T> {

    private final S3ConfigurationProvider<T> configProvider;
    private S3HealthCheckType s3HealthCheckType = NONE;
    private Iterable<BucketNameProvider<T>> bucketNameProviders;
    private OpenTelemetry openTelemetry;

    private Builder() {
      configProvider = null;
    }

    private Builder(S3ConfigurationProvider<T> configProvider) {
      this.configProvider = configProvider;
    }

    @Override
    public <C extends Configuration> S3HealthCheckBuilder<C> withConfigurationProvider(
        S3ConfigurationProvider<C> configurationProvider) {
      return new Builder<>(configurationProvider);
    }

    @Override
    public FinalBuilder<T> withOpenTelemetry(OpenTelemetry openTelemetry) {
      this.openTelemetry = openTelemetry;
      return this;
    }

    @Override
    public S3Bundle<T> build() {
      return new S3Bundle<>(configProvider, openTelemetry, s3HealthCheckType, bucketNameProviders);
    }

    @Override
    public FinalBuilder<T> withHealthCheck(Iterable<BucketNameProvider<T>> bucketNameProviders) {
      this.s3HealthCheckType = INTERNAL;
      this.bucketNameProviders = Objects.requireNonNull(bucketNameProviders);
      return this;
    }

    @Override
    public FinalBuilder<T> withExternalHealthCheck(
        Iterable<BucketNameProvider<T>> bucketNameProviders) {
      this.s3HealthCheckType = EXTERNAL;
      this.bucketNameProviders = Objects.requireNonNull(bucketNameProviders);
      return this;
    }

    @Override
    public FinalBuilder<T> withHealthCheck(String bucketName) {
      this.s3HealthCheckType = INTERNAL;
      this.bucketNameProviders = Collections.singleton((T t) -> bucketName);
      return this;
    }

    @Override
    public FinalBuilder<T> withExternalHealthCheck(String bucketName) {
      this.s3HealthCheckType = EXTERNAL;
      this.bucketNameProviders = Collections.singleton((T t) -> bucketName);
      return this;
    }
  }

  public interface BucketNameProvider<C extends Configuration> extends Function<C, String> {}
}
