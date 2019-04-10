package org.sdase.commons.server.opa.config;

import io.dropwizard.Configuration;
import java.util.function.Function;

/**
 * Provides the {@link OpaConfig} for the {@link org.sdase.commons.server.opa.OpaBundle}
 *
 * @param <C> the type of the specific {@link Configuration} used in the application
 */
public interface OpaConfigProvider<C extends Configuration> extends Function<C, OpaConfig> {

}
