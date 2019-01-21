package org.sdase.commons.server.morphia;

import io.dropwizard.Configuration;

import java.util.function.Function;

/**
 * Provides the configuration for the mongo bundle {@link MongoConfiguration}, e.g. {@code MyAppConfig::getMongo}
 *
 * @param <C> the type of the applications configuration class
 */
@FunctionalInterface
public interface MongoConfigurationProvider<C extends Configuration> extends Function<C, MongoConfiguration> {
}
