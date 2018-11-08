package org.sdase.commons.server.auth.config;

import org.sdase.commons.server.auth.AuthBundle;
import io.dropwizard.Configuration;

import java.util.function.Function;


/**
 * Provides the {@link AuthConfig} for the {@link AuthBundle}
 *
 * @param <C> the type of the specific {@link Configuration} used in the application
 */
public interface AuthConfigProvider<C extends Configuration> extends Function<C, AuthConfig> {

}
