package org.sdase.commons.server.hibernate;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import java.util.function.Function;

/**
 * Provides the configuration of a database through a {@link DataSourceFactory}, e.g. {@code MyAppConfig::getDatabase}
 *
 * @param <C> the type of the applications configuration class
 */
@FunctionalInterface
public interface DatabaseConfigurationProvider<C extends Configuration> extends Function<C, DataSourceFactory> {
}
