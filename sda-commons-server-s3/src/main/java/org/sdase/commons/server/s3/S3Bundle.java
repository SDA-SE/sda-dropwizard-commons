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
import javax.validation.constraints.NotNull;

public class S3Bundle<C extends Configuration> implements ConfiguredBundle<C> {

  private final S3ConfigurationProvider<C> configurationProvider;
  private AmazonS3 s3Client;

  private S3Bundle(S3ConfigurationProvider<C> configurationProvider) {
    this.configurationProvider = configurationProvider;
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

    s3Client =
        AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(s3Configuration.getEndpoint(), region))
            .withPathStyleAccessEnabled(true)
            .withClientConfiguration(clientConfiguration)
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .build();

    environment.lifecycle().manage(onShutdown(() -> s3Client.shutdown()));
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
     */
    <C extends Configuration> FinalBuilder<C> withConfigurationProvider(
        @NotNull S3ConfigurationProvider<C> configurationProvider);
  }

  public interface FinalBuilder<C extends Configuration> {

    /**
     * Builds the S3 bundle
     *
     * @return S3 bundle
     */
    S3Bundle<C> build();
  }

  public static class Builder<T extends Configuration> implements InitialBuilder, FinalBuilder<T> {

    private final S3ConfigurationProvider<T> configProvider;

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
    public S3Bundle<T> build() {
      return new S3Bundle<>(configProvider);
    }
  }
}
