package org.sdase.commons.server.morphia;

import io.dropwizard.Configuration;
import java.util.function.Function;

/**
 * Provides the {@link MongoConfiguration} for the {@link MorphiaBundle}, e.g. {@code
 * MyAppConfig::getMongo}
 *
 * @param <C> the type of the applications configuration class
 */
@FunctionalInterface
public interface MongoConfigurationProvider<C extends Configuration>
    extends Function<C, MongoConfiguration> {}
