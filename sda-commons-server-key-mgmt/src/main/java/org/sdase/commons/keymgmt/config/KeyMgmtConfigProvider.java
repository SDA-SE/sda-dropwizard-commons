package org.sdase.commons.keymgmt.config;

import io.dropwizard.Configuration;
import java.util.function.Function;
import org.sdase.commons.keymgmt.KeyMgmtBundle;

/**
 * Provides the {@link KeyMgmtConfig} for the {@link KeyMgmtBundle}
 *
 * @param <C> the type of the specific {@link Configuration} used in the application
 */
public interface KeyMgmtConfigProvider<C extends Configuration>
    extends Function<C, KeyMgmtConfig> {}
