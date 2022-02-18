package org.sdase.commons.server.s3;

import static org.sdase.commons.server.dropwizard.lifecycle.ManagedShutdownListener.onShutdown;

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
import io.opentracing.Tracer;
import io.opentracing.contrib.aws.TracingRequestHandler;
import io.opentracing.util.GlobalTracer;
import javax.validation.constraints.NotNull;
import org.sdase.commons.server.s3.health.ExternalS3HealthCheck;
import org.sdase.commons.server.s3.health.S3HealthCheck;

public class S3Bundle<C extends Configuration> implements ConfiguredBundle<C> {

  public static final String S3_HEALTH_CHECK_NAME = "s3Connection";
  public static final String S3_EXTERNAL_HEALTH_CHECK_NAME = "s3ConnectionExternal";

  private final S3ConfigurationProvider<C> configurationProvider;
  private final Tracer tracer;
  private final boolean healthCheck;
  private final String[] bucketName;
  private AmazonS3 s3Client;

  private S3Bundle(
      S3ConfigurationProvider<C> configurationProvider,
      Tracer tracer,
      boolean healthCheck,
      String[] bucketName) {
    this.configurationProvider = configurationProvider;
    this.tracer = tracer;
    this.healthCheck = healthCheck;
    this.bucketName = bucketName;
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

    Tracer currentTracer = tracer == null ? GlobalTracer.get() : tracer;

    s3Client =
        AmazonS3ClientBuilder.standard()
            .withRequestHandlers(new TracingRequestHandler(currentTracer))
            .withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(s3Configuration.getEndpoint(), region))
            .withPathStyleAccessEnabled(true)
            .withClientConfiguration(clientConfiguration)
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .build();

    environment.lifecycle().manage(onShutdown(s3Client::shutdown));

    if (healthCheck) {
      environment
          .healthChecks()
          .register(S3_HEALTH_CHECK_NAME, new S3HealthCheck(s3Client, bucketName));
    } else {
      environment
          .healthChecks()
          .register(S3_EXTERNAL_HEALTH_CHECK_NAME, new ExternalS3HealthCheck(s3Client, bucketName));
    }
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
    <C extends Configuration> FinalBuilder<C> withConfigurationProvider(
        @NotNull S3ConfigurationProvider<C> configurationProvider);
  }

  public interface FinalBuilder<C extends Configuration> {

    /**
     * Specifies a custom tracer to use. If no tracer is specified, the {@link GlobalTracer} is
     * used.
     *
     * @param tracer The tracer to use
     * @return the same builder
     */
    FinalBuilder<C> withTracer(Tracer tracer);

    /**
     * Adds an internal health check for an S3 connection against one or more buckets. Should not be
     * used with {@link #withExternalHealthCheck(String...)}.
     *
     * @param bucketNames the bucket to connect to.
     * @return the same builder instance
     */
    FinalBuilder<C> withHealthCheck(String... bucketNames);

    /**
     * Adds an external health check for an S3 connection against one or more buckets. Should not be
     * used with {@link #withHealthCheck(String...)}.
     *
     * @param bucketNames the bucket to connect to.
     * @return the same builder instance
     */
    FinalBuilder<C> withExternalHealthCheck(String... bucketNames);

    /**
     * Builds the S3 bundle
     *
     * @return S3 bundle
     */
    S3Bundle<C> build();
  }

  public static class Builder<T extends Configuration> implements InitialBuilder, FinalBuilder<T> {

    private final S3ConfigurationProvider<T> configProvider;
    private String[] bucketNames;
    private Tracer tracer;
    private boolean healthCheck;
    private boolean externalHealthCheck;

    private Builder() {
      configProvider = null;
    }

    private Builder(S3ConfigurationProvider<T> configProvider) {
      this.configProvider = configProvider;
    }

    @Override
    public <C extends Configuration> FinalBuilder<C> withConfigurationProvider(
        S3ConfigurationProvider<C> configurationProvider) {
      return new Builder<>(configurationProvider);
    }

    @Override
    public FinalBuilder<T> withTracer(Tracer tracer) {
      this.tracer = tracer;
      return this;
    }

    @Override
    public FinalBuilder<T> withHealthCheck(String[] bucketNames) {
      this.bucketNames = bucketNames;
      this.healthCheck = true;
      if (this.externalHealthCheck) {
        throw new IllegalArgumentException("There is already an external health check defined.");
      }
      return this;
    }

    @Override
    public FinalBuilder<T> withExternalHealthCheck(String[] bucketNames) {
      this.bucketNames = bucketNames;
      this.externalHealthCheck = true;
      if (this.healthCheck) {
        throw new IllegalArgumentException("There is already a health check defined.");
      }
      return this;
    }

    @Override
    public S3Bundle<T> build() {
      return new S3Bundle<>(configProvider, tracer, healthCheck, bucketNames);
    }
  }
}
