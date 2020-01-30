package org.sdase.commons.server.circuitbreaker.builder;

import io.dropwizard.Configuration;
import org.sdase.commons.server.circuitbreaker.CircuitBreakerConfiguration;
import org.sdase.commons.server.circuitbreaker.CircuitBreakerConfigurationProvider;

public interface CircuitBreakerConfigurationBuilder<T extends Configuration> {

  /**
   * Set a custom configuration for the circuit breaker.
   *
   * @param config The config to apply.
   * @return the same builder instance
   */
  CircuitBreakerExceptionBuilder<T> withCustomConfig(CircuitBreakerConfiguration config);

  /**
   * Set a provider for a custom configuration for the circuit breaker.
   *
   * @param provider The provider to load the config from the Dropwizard config.
   * @return the same builder instance
   */
  CircuitBreakerExceptionBuilder<T> withConfigProvider(
      CircuitBreakerConfigurationProvider<T> provider);

  /**
   * Use the default configuration of the bundle for the circuit breaker.
   *
   * @return the same builder instance
   */
  CircuitBreakerExceptionBuilder<T> withDefaultConfig();
}
