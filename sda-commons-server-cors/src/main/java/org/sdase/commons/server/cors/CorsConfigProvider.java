package org.sdase.commons.server.cors;

import io.dropwizard.Configuration;
import java.util.function.Function;

/**
 * Provides the {@link CorsConfiguration} for the {@link CorsBundle}
 *
 * @param <C> the type of the specific {@link Configuration} used in the application
 */
@FunctionalInterface
public interface CorsConfigProvider<C extends Configuration>
    extends Function<C, CorsConfiguration> {}
