package org.sdase.commons.server.circuitbreaker;

import static io.github.resilience4j.prometheus.collectors.CircuitBreakerMetricsCollector.ofCircuitBreakerRegistry;
import static org.sdase.commons.server.dropwizard.lifecycle.ManagedShutdownListener.onShutdown;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.prometheus.collectors.CircuitBreakerMetricsCollector;
import io.prometheus.client.CollectorRegistry;
import org.sdase.commons.server.circuitbreaker.builder.CircuitBreakerBuilder;
import org.sdase.commons.server.circuitbreaker.builder.CircuitBreakerConfigurationBuilder;

/**
 * Bundle that provides access to an implementation of the circuit breaker pattern to handle
 * failures of downstream services. The bundle uses resilience4j and exposes instances of {@code
 * CircuitBreaker}.
 *
 * <p>The bundle allows to create circuit breakers, wrap classes in circuit breakers and registers
 * prometheus metrics.
 */
public class CircuitBreakerBundle<T extends Configuration> implements ConfiguredBundle<T> {

  private final CircuitBreakerConfigurationProvider<T> configurationProvider;
  private final Class<? extends Throwable>[] recordedErrorClasses;
  private final Class<? extends Throwable>[] ignoredErrorClasses;
  private CircuitBreakerRegistry registry;
  private T configuration;

  private CircuitBreakerBundle(
      CircuitBreakerConfigurationProvider<T> configurationProvider,
      Class<? extends Throwable>[] recordedErrorClasses,
      Class<? extends Throwable>[] ignoredErrorClasses) {
    this.configurationProvider = configurationProvider;
    this.recordedErrorClasses = recordedErrorClasses;
    this.ignoredErrorClasses = ignoredErrorClasses;
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // Nothing to initialize
  }

  @Override
  public void run(T configuration, Environment environment) {
    this.configuration = configuration;
    CircuitBreakerConfiguration circuitBreakerConfiguration =
        configurationProvider.apply(configuration);
    CircuitBreakerConfig config =
        CircuitBreakerConfig.custom()
            .enableAutomaticTransitionFromOpenToHalfOpen()
            .failureRateThreshold(circuitBreakerConfiguration.getFailureRateThreshold())
            .ringBufferSizeInClosedState(
                circuitBreakerConfiguration.getRingBufferSizeInClosedState())
            .ringBufferSizeInHalfOpenState(
                circuitBreakerConfiguration.getRingBufferSizeInHalfOpenState())
            .waitDurationInOpenState(circuitBreakerConfiguration.getWaitDurationInOpenState())
            .recordExceptions(recordedErrorClasses)
            .ignoreExceptions(ignoredErrorClasses)
            .build();
    registry = CircuitBreakerRegistry.of(config);

    CollectorRegistry collectorRegistry = CollectorRegistry.defaultRegistry;
    CircuitBreakerMetricsCollector circuitBreakerMetricsCollector =
        ofCircuitBreakerRegistry(registry);
    collectorRegistry.register(circuitBreakerMetricsCollector);

    environment
        .lifecycle()
        .manage(onShutdown(() -> collectorRegistry.unregister(circuitBreakerMetricsCollector)));
  }

  /**
   * Create a new circuit breaker builder.
   *
   * @param name The name of the circuit breaker, used in the metrics. Multiple calls with the same
   *     name return the same circuit breaker
   * @return A circuit breaker builder.
   */
  public CircuitBreakerConfigurationBuilder<Configuration> createCircuitBreaker(String name) {
    return new CircuitBreakerBuilder<>(name, registry, configuration);
  }

  /**
   * Returns the circuit breaker registry created by the bundle.
   *
   * @return The circuit breaker registry.
   */
  public CircuitBreakerRegistry getRegistry() {
    if (registry == null) {
      throw new IllegalStateException(
          "Circuit breaker registry accessed to early, can't be accessed before run.");
    }

    return registry;
  }

  /**
   * Builder for creating a new instance.
   *
   * @return Builder
   */
  public static ConfigurationBuilder builder() {
    return new Builder<>();
  }

  public interface ConfigurationBuilder {

    /**
     * Provide a custom default configuration for circuit breakers created using the bundle.
     *
     * @param config The circuit breaker configuration.
     * @param <T> Type of the Dropwizard configuration.
     * @return the same builder instance
     */
    <T extends Configuration> ExceptionBuilder<T> withCustomConfig(
        CircuitBreakerConfiguration config);

    /**
     * Use a provider for a custom default configuration for circuit breakers created using the
     * bundle.
     *
     * @param provider Provider to extract the circuit breaker configuration from the Dropwizard
     *     configuration.
     * @param <T> Type of the Dropwizard configuration.
     * @return the same builder instance
     */
    <T extends Configuration> ExceptionBuilder<T> withConfigProvider(
        CircuitBreakerConfigurationProvider<T> provider);

    /**
     * Use the default configuration for circuit breakers created using the bundle.
     *
     * @param <T> Type of the Dropwizard configuration.
     * @return the same builder instance
     */
    <T extends Configuration> ExceptionBuilder<T> withDefaultConfig();
  }

  public interface ExceptionBuilder<T extends Configuration> extends FinalBuilder<T> {

    /**
     * Configures a list of error classes that are recorded as a failure and thus increase the
     * failure rate. Any exception matching or inheriting from one of the list should count as a
     * failure, unless ignored
     *
     * @param errorClasses the error classes that are recorded
     * @return the same builder instance
     */
    ExceptionBuilder<T> recordExceptions(Class<? extends Throwable>... errorClasses);

    /**
     * Configures a list of error classes that are ignored as a failure and thus do not increase the
     * failure rate. Any exception matching or inheriting from one of the list will not count as a
     * failure, even if marked via record.
     *
     * @param errorClasses the error classes that are ignored
     * @return the same builder instance
     */
    ExceptionBuilder<T> ignoreExceptions(Class<? extends Throwable>... errorClasses);
  }

  public interface FinalBuilder<T extends Configuration> {

    /**
     * Create a new instance.
     *
     * @return A new {@code CircuitBreakerBundle}
     */
    CircuitBreakerBundle<T> build();
  }

  public static class Builder<C extends Configuration>
      implements ConfigurationBuilder, ExceptionBuilder<C>, FinalBuilder<C> {

    private CircuitBreakerConfigurationProvider<C> configurationProvider;
    private Class<? extends Throwable>[] recordedErrorClasses;
    private Class<? extends Throwable>[] ignoredErrorClasses;

    private Builder() {}

    private Builder(CircuitBreakerConfigurationProvider<C> provider) {
      this.configurationProvider = provider;
    }

    @Override
    public <T extends Configuration> ExceptionBuilder<T> withCustomConfig(
        CircuitBreakerConfiguration config) {
      return new Builder<>(c -> config);
    }

    @Override
    public <T extends Configuration> ExceptionBuilder<T> withConfigProvider(
        CircuitBreakerConfigurationProvider<T> provider) {
      return new Builder<>(provider);
    }

    @Override
    public <T extends Configuration> ExceptionBuilder<T> withDefaultConfig() {
      return new Builder<>(c -> new CircuitBreakerConfiguration());
    }

    @Override
    public ExceptionBuilder<C> recordExceptions(Class<? extends Throwable>... errorClasses) {
      recordedErrorClasses = errorClasses;
      return this;
    }

    @Override
    public ExceptionBuilder<C> ignoreExceptions(Class<? extends Throwable>... errorClasses) {
      ignoredErrorClasses = errorClasses;
      return this;
    }

    @Override
    public CircuitBreakerBundle<C> build() {
      return new CircuitBreakerBundle<>(
          configurationProvider, recordedErrorClasses, ignoredErrorClasses);
    }
  }
}
