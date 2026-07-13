package org.sdase.commons.server.cors;

import io.dropwizard.core.Configuration;

/**
 * Provides the {@link CorsConfiguration} for the {@link CorsBundle}
 *
 * @param <C> the type of the specific {@link Configuration} used in the application
 * @deprecated This class will be replaced by {@link
 *     org.sdase.commons.server.dropwizard.bundles.CorsBundle.CorsConfigProvider} when removing the
 *     module {@code sda-commons-server-cors}. To prepare for the upcoming breaking change, update
 *     all references to {@link
 *     org.sdase.commons.server.dropwizard.bundles.CorsBundle.CorsConfigProvider} and remove direct
 *     dependencies to {@code sda-commons-server-cors}.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("java:S2176") // intentionally the same name until removed
@FunctionalInterface
public interface CorsConfigProvider<C extends Configuration>
    extends org.sdase.commons.server.dropwizard.bundles.CorsBundle.CorsConfigProvider<C> {}
