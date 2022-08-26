package org.sdase.commons.client.jersey;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import java.util.function.Function;
import org.sdase.commons.client.jersey.builder.PlatformClientBuilder;
import org.sdase.commons.client.jersey.error.ClientRequestExceptionMapper;
import org.sdase.commons.client.jersey.filter.ContainerRequestContextHolder;

/** A bundle that provides Jersey clients with appropriate configuration for the SDA Platform. */
public class JerseyClientBundle<C extends Configuration> implements ConfiguredBundle<C> {

  private ClientFactory clientFactory;

  private boolean initialized;

  private Function<C, String> consumerTokenProvider;
  private OpenTelemetry openTelemetry;

  public static InitialBuilder<Configuration> builder() {
    return new Builder<>();
  }

  private JerseyClientBundle(
      Function<C, String> consumerTokenProvider, OpenTelemetry openTelemetry) {
    this.consumerTokenProvider = consumerTokenProvider;
    this.openTelemetry = openTelemetry;
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // no initialization needed here, we need the environment to initialize the client
  }

  @Override
  public void run(C configuration, Environment environment) {
    // Initialize a telemetry instance if not set.
    OpenTelemetry currentTelemetryInstance =
        this.openTelemetry == null ? GlobalOpenTelemetry.get() : this.openTelemetry;

    this.clientFactory =
        new ClientFactory(
            environment, consumerTokenProvider.apply(configuration), currentTelemetryInstance);
    environment.jersey().register(ContainerRequestContextHolder.class);
    environment.jersey().register(ClientRequestExceptionMapper.class);
    initialized = true;
  }

  /**
   * @return a factory to build clients that can be either used to call within the SDA platform or
   *     to call external services
   * @throws IllegalStateException if called before {@link
   *     io.dropwizard.Application#run(Configuration, Environment)} because the factory has to be
   *     initialized within {@link ConfiguredBundle#run(Object, Environment)}
   */
  public ClientFactory getClientFactory() {
    if (!initialized) {
      throw new IllegalStateException(
          "Clients can be build in run(C, Environment), not in initialize(Bootstrap)");
    }
    return clientFactory;
  }

  //
  // Builder
  //

  public interface InitialBuilder<C extends Configuration> extends FinalBuilder<C> {
    /**
     * @param consumerTokenProvider A provider for the header value of the Http header {@value
     *     org.sdase.commons.shared.tracing.ConsumerTracing#TOKEN_HEADER} that will be send with
     *     each client request configured with {@link PlatformClientBuilder#enableConsumerToken()}.
     *     If no such provider is configured, {@link PlatformClientBuilder#enableConsumerToken()}
     *     will fail.
     * @param <C1> the type of the applications configuration class
     * @return a builder instance for further configuration
     */
    <C1 extends Configuration> FinalBuilder<C1> withConsumerTokenProvider(
        ConsumerTokenProvider<C1> consumerTokenProvider);
  }

  public interface FinalBuilder<C extends Configuration> {
    /**
     * Specifies a custom telemetry instance to use. If no instance is specified, the {@link
     * GlobalOpenTelemetry} is used.
     *
     * @param openTelemetry The telemetry instance to use
     * @return the same builder
     */
    JerseyClientBundle.FinalBuilder<C> withTelemetryInstance(OpenTelemetry openTelemetry);

    JerseyClientBundle<C> build();
  }

  public static class Builder<C extends Configuration>
      implements InitialBuilder<C>, FinalBuilder<C> {

    private ConsumerTokenProvider<C> consumerTokenProvider = (C c) -> null;
    private OpenTelemetry openTelemetry;

    private Builder() {}

    private Builder(ConsumerTokenProvider<C> consumerTokenProvider) {
      this.consumerTokenProvider = consumerTokenProvider;
    }

    @Override
    public <C1 extends Configuration> FinalBuilder<C1> withConsumerTokenProvider(
        ConsumerTokenProvider<C1> consumerTokenProvider) {
      return new Builder<>(consumerTokenProvider);
    }

    @Override
    public JerseyClientBundle.FinalBuilder<C> withTelemetryInstance(OpenTelemetry openTelemetry) {
      this.openTelemetry = openTelemetry;
      return this;
    }

    @Override
    public JerseyClientBundle<C> build() {
      return new JerseyClientBundle<>(consumerTokenProvider, openTelemetry);
    }
  }

  /**
   * Provides the consumer token that is added to outgoing requests from the configuration.
   *
   * @param <C> the type of the applications {@link Configuration} class
   */
  public interface ConsumerTokenProvider<C extends Configuration> extends Function<C, String> {}
}
