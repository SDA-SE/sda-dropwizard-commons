package org.sdase.commons.server.s3;

import static org.sdase.commons.server.dropwizard.lifecycle.ManagedShutdownListener.onShutdown;
import static org.sdase.commons.server.s3.health.S3HealthCheckType.EXTERNAL;
import static org.sdase.commons.server.s3.health.S3HealthCheckType.INTERNAL;
import static org.sdase.commons.server.s3.health.S3HealthCheckType.NONE;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.validation.constraints.NotNull;
import org.sdase.commons.server.s3.health.ExternalS3HealthCheck;
import org.sdase.commons.server.s3.health.S3HealthCheck;
import org.sdase.commons.server.s3.health.S3HealthCheckType;

public class S3Bundle<C extends Configuration> implements ConfiguredBundle<C> {

  public static final String S3_HEALTH_CHECK_NAME = "s3Connection";
  public static final String S3_EXTERNAL_HEALTH_CHECK_NAME = "s3ConnectionExternal";

  private final S3ConfigurationProvider<C> configurationProvider;
  private final OpenTelemetry openTelemetry;
  private final S3HealthCheckType s3HealthCheckType;
  private final Iterable<BucketNameProvider<C>> bucketNameProviders;
  private AmazonS3 s3Client;

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
  public void run(C configuration, Environment environment) throws Exception {
    S3Configuration s3Configuration = configurationProvider.apply(configuration);
    String region = s3Configuration.getRegion();

    if (region == null || "".equals(region)) {
      region = Regions.DEFAULT_REGION.getName();
    }

    AWSCredentials credentials =
        new BasicAWSCredentials(s3Configuration.getAccessKey(), s3Configuration.getSecretKey());
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.setSignerOverride(s3Configuration.getSignerOverride());

    // Initialize a telemetry instance if not set.
    OpenTelemetry currentTelemetryInstance =
        this.openTelemetry == null ? GlobalOpenTelemetry.get() : this.openTelemetry;
    s3Client =
        AmazonS3ClientBuilder.standard()
            .withRequestHandlers(new TracingRequestHandler(currentTelemetryInstance))
            .withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(s3Configuration.getEndpoint(), region))
            .withPathStyleAccessEnabled(true)
            .withClientConfiguration(clientConfiguration)
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .build();

    environment.lifecycle().manage(onShutdown(s3Client::shutdown));

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

  private boolean isHealthCheckEnabled() {
    return !NONE.equals(this.s3HealthCheckType);
  }

  public AmazonS3 getClient() {
    if (s3Client == null) {
      throw new IllegalStateException("S3 client accessed to early, can't be accessed before run.");
    }

    return s3Client;
  }

  //
  // Builder
  //
  public static InitialBuilder builder() {
    return new Builder();
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
     * @param bucketNameProviders
     * @return the builder instance
     */
    FinalBuilder<C> withHealthCheck(Iterable<BucketNameProvider<C>> bucketNameProviders);

    /**
     * Adds an external health check for an S3 connection against one or more buckets.
     *
     * @param bucketNameProviders
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
    FinalBuilder<C> withTelemetryInstance(OpenTelemetry openTelemetry);

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
    public FinalBuilder<T> withTelemetryInstance(OpenTelemetry openTelemetry) {
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
