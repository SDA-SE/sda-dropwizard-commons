package org.sdase.commons.server.prometheus;

import io.dropwizard.core.Configuration;
import java.util.function.Function;
import org.sdase.commons.server.prometheus.config.PrometheusConfiguration;

/**
 * Provides the configuration for the prometheus bundle {@link PrometheusConfiguration}, e.g. {@code
 * MyAppConfig::getPrometheus}
 *
 * @param <C> the type of the applications configuration class
 */
@FunctionalInterface
public interface PrometheusConfigurationProvider<C extends Configuration>
    extends Function<C, PrometheusConfiguration> {}
