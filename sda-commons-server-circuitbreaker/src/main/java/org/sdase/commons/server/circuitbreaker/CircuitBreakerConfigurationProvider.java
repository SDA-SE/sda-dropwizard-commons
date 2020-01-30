package org.sdase.commons.server.circuitbreaker;

import io.dropwizard.Configuration;
import java.util.function.Function;

/**
 * Provides the {@link CircuitBreakerConfiguration} for the {@link CircuitBreakerBundle}
 *
 * @param <C> the type of the specific {@link Configuration} used in the application
 */
public interface CircuitBreakerConfigurationProvider<C extends Configuration>
    extends Function<C, CircuitBreakerConfiguration> {}
