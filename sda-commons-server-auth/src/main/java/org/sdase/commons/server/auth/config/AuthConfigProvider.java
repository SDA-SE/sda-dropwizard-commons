package org.sdase.commons.server.auth.config;

import io.dropwizard.Configuration;
import java.util.function.Function;
import org.sdase.commons.server.auth.AuthBundle;

/**
 * Provides the {@link AuthConfig} for the {@link AuthBundle}
 *
 * @param <C> the type of the specific {@link Configuration} used in the application
 */
public interface AuthConfigProvider<C extends Configuration> extends Function<C, AuthConfig> {}
